package com.meteorcode.pathway.io
import java.io.{
  File,
  InputStream,
  OutputStream,
  BufferedOutputStream,
  BufferedInputStream,
  FileInputStream,
  FileOutputStream,
  IOException
}
import java.util.{
  List,
  ArrayList
}
import java.nio.charset.Charset

import java.util.zip.{
  ZipFile,
  ZipEntry,
  ZipInputStream
}
import java.util.jar.{
  JarFile,
  JarEntry,
  JarInputStream
}
import java.util.Collections

import scala.io.Source
import scala.collection.JavaConversions._

/**
 * <p>An abstraction wrapping a file in the filesystem.</p>
 *
 * <p>FileHandle paths are relative to the game's Assets directory. This is a "virtual directory" managed by the game's
 * {@link com.meteorcode.pathway.io.ResourceManager ResourceManager}. Zip and Jar files within the virtual assets directory are
 * treated as though they were directories, rather than as flat files, so files within a Zip or Jar file may be accessed as though
 * that file was a directory containing them. </p>
 *
 * <p>On the desktop, the Assets directory is located at <approot>/assets, although a different directory may be specified when the
 * game's ResourceManager is initialized. On Android, the game's virtual Assets directory encompasses the files stored within the
 * game's APK as assets, and files stored in the application's associated portion of the device's internal storage. The ResourceManager
 * "fuses" thetop-level index of both of these locations  into one virtual directory tree, to which all FileHandle paths are relative.</p>
 *
 * <p> All FileHandles are backed by {@link java.io.InputStream InputStream}s and {@link java.io.OutputStream OutputStream}s,
 * so reading and writing to a file should be consistent across operating systems, filesystem locations, and file types.</p>
 *
 * @author Hawk Weisman
 */
abstract class FileHandle {
  /** Returns true if the file exists. */
  def exists(): Boolean

  /** Returns true if this file is a directory. */
  def isDirectory(): Boolean

  /** Returns true if this FileHandle represents something that can be written to */
  def writeable(): Boolean

  /** <p>Returns the path to this FileHandle.</p>
   * <p>All paths are treated as into Unix-style paths for cross-platform purposes.
   */
  def path: String

  /**
   *  <p>Returns a list containing FileHandles to the contents of FileHandle .</p>
   *  <p> Returns an empty list if this file is not a directory or does not have contents.</p>
   */
  def list: List[FileHandle]

  /**
   *  <p>Returns a list containing FileHandles to the contents of this FileHandle with the specified suffix.</p>
   *  <p> Returns an empty list if this file is not a directory or does not have contents.</p>
   *  @param suffix
   */
  def list(suffix: String): java.util.List[FileHandle] = list.filter(entry => entry.path.endsWith(suffix))

  /** Returns a stream for reading this file as bytes.
   *  @throws IOException if the file does not exist or is a directory.
   */
  @throws(classOf[IOException])
  def read(): InputStream

  /** Returns a buffered stream for reading this file as bytes.
   *  @throws IOException if the file does not exist or is a directory.
   */
  @throws(classOf[IOException])
  def read(bufferSize: Integer): BufferedInputStream = new BufferedInputStream (read(), bufferSize)

  /** Reads the entire file into a string using the platform's default charset.
	 *  @throws IOException if the file does not exist or is a directory.
   */
  @throws(classOf[IOException])
  def readString(): String = readString(Charset.defaultCharset())

  /** Reads the entire file into a string using the specified charset.*/
  @throws(classOf[IOException])
  def readString(charset: Charset): String = Source.fromInputStream(read).mkString

  /** Returns an {@link java.io.OutputStream OutputStream} for writing to this file.
   * @return an {@link java.io.OutputStream OutputStream} for writing to this file, or null if this file is not writeable.
   * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
   */
   def write(append: Boolean): OutputStream

  /** Returns an {@link java.io.BufferedOutputStream BufferedOutputStream} for writing to this file.
   * @return an {@link java.io.BufferedOutputStream BufferedOutputStream} for writing to this file, or null if this file is not writeable.
   * @param bufferSize The size of the buffer
   * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
   */
   def write(bufferSize: Integer, append: Boolean): BufferedOutputStream = if (writeable) { new BufferedOutputStream(write(append), bufferSize) } else null

   /** <p>Writes the specified string to the file using the default charset.</p>
    * <p>Throws an {@link java.io.IOException IOException} if the FileHandle represents something that is not writeable; yes, I am aware
    * that having some of these methods return null and others throw exceptions is Wrong and I should feel Bad,
    * I wanted them to return an {@link scala.Option Option}, but Max wouldn't let me.</p>
    * @param string the string to write to the file
    * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @throws IOException if this file is not writeable
    */
   @throws(classOf[IOException])
   def writeString (string: String, append: Boolean): Unit = if (writeable) { writeString(string, Charset.defaultCharset(), append) } else { throw new IOException("FileHandle " + path + " is not writeable.") }

   /** Writes the specified string to the file using the specified  charset.
    * <p>Throws an {@link java.io.IOException IOException} if the FileHandle represents something that is not writeable; yes, I am aware
    * that having some of these methods return null and others throw exceptions is Wrong and I should feel Bad,
    * I wanted them to return an {@link scala.Option Option}, but Max wouldn't let me.</p>
    * @param string the string to write to the file
    * @param charset the {@link java.nio.charset.Charset charset} to use while writing to the file
    * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
    * @throws IOException if this file is not writeable
    */
   @throws(classOf[IOException])
   def writeString (string: String, charset: Charset, append: Boolean): Unit = if (writeable) { write(append).write(string.getBytes(charset)) } else { throw new IOException("FileHandle " + path + " is not writeable.") }
}

class DesktopFileHandle (pathTo: String) extends FileHandle {
  protected val file = new File(pathTo)

  def path = file getPath //TODO: Add code to coerce Windows paths into Unix-style paths as documented (This Sounds Like A Job For regular expressions)
  def exists: Boolean = file exists
  def isDirectory: Boolean = file isDirectory
  def writeable: Boolean = exists && file.canWrite

  def read: InputStream = {
    if (!exists) throw new IOException("Could not read file:" + path + ", the requested file does not exist.")
    else if (isDirectory) throw new IOException("Could not read file:" + path + ", the requested file is a directory.")
    else new FileInputStream(file)
  }

  def list: List[FileHandle] = {
    if (isDirectory) {
      var result = new java.util.ArrayList[FileHandle]
      for (item <- file.list) {
        item.split('.').drop(1).last match {
          case "jar" => result add new JarFileHandle(item)
          case "zip" => result add new ZipFileHandle(item)
          case _ => result add new DesktopFileHandle(item)
        }
      }
      result
    } else Collections.emptyList()
  }

  def write(append: Boolean) = if (writeable) { new FileOutputStream(file, append) } else null
}

class ZipFileHandle (pathTo: String) extends FileHandle {
  /*
  Let's take a moment to discuss how Java's Zip API is Not My Favourite Thing.

  In the API, a Zip file is represented by a ZipFile object. Files WITHIN that
  Zip file, however, are represented by ZipEntries. Unfortunately, the only way
  to coerce Java to give you an InputStream for any given file in a Zip is to
  call a method on the parent ZipFile, passing the ZipEntry as an argument.
  This means that if you have a ZipEntry that you want to read, you also need
  a pointer to the ZipFile it lives in if you want to do anything useful with it.
  As any sane person can tell, this is not the right thing.

  This incredibly brain-dead design is why we have to have ZipFileHandles AND
  ZipEntryFileHandles - we can't treat ZipFiles as though they were regular
  files, since we want to allow them to be accessed transparently as though
  they were directories - but we also can't handle files within those Zip files
  using FileHandles OR ZipFileHandles.

  To add insult to injury, I'm pretty sure this idiocy isn't the Zip ile format's
  fault - Python's 'zipfile' module basically allows you to treat files within a Zip
  archive transparently as though they were regular files in the filesystem, because
  (unlike java.util.zip), it wasn't designed by a committee of people dead-set on
  making my day miserable. Also, documentation for Python's module is much better.

  In short, I hate java.util.zip.
  */
  private val file = new File(pathTo)
  private val zipfile = new ZipFile(file)

  def path = file getPath
  def exists: Boolean = file exists
  def isDirectory: Boolean = true // Remember, we are pretending that zips are directories
  def writeable = false // Zips can never be written to (at least by java.util.zip)

  @throws(classOf[IOException])
  def list: List[FileHandle] = {
    var result = new ArrayList[FileHandle]
    try {
      val entries = zipfile.entries
      // furthermore, I also loathe java.util.zip for making me use the braindead
      // Enumeration<T> class which appears to be a dumb knockoff of Iterator created
      // specifically for use in ZipFile just to make it EVEN WORSE
      // I HATE JAVA
     while (entries hasMoreElements) result.add( new ZipEntryFileHandle(entries.nextElement(), zipfile, path) )
     result
    } catch {
      // Don't close my ZipFile while I'm getting its' entries! Geez!
      case e: IllegalStateException => throw new IOException ("Could not list ZipFile entries, file " + path + " appears to have been closed.", e)
    }
  }

  @throws(classOf[IOException])
  def list(suffix: String): List[FileHandle] = list.filter(entry => entry.path.endsWith(suffix))
  def write(append: Boolean) = null

}

class ZipEntryFileHandle (private val entry: ZipEntry, private val parent: ZipFile, private val pathTo: String) extends FileHandle {
  def writeable = false // Zip files cannot be written to :c
  def exists = true // if this ZipEntry was found in the ZipFile, it is Real And Has Been Proven To Exist
                    // (this is okay because ZipEntries are apparently un-deleteable; HAVE I MENTIONED HOW MUCH I HATE java.util.zip LATELY?)
  def isDirectory = entry isDirectory
  def path = pathTo + entry.getName//TODO: coerce paths into Unix paths.

  def read: InputStream = {
    if (!exists) throw new IOException("Could not read file:" + path + ", the requested file does not exist.")
    else if (isDirectory) throw new IOException("Could not read file:" + path + ", the requested file is a directory.")
    else parent.getInputStream(entry)
  }

  def list: List[FileHandle] = {
    if (isDirectory) {
      var result = new ArrayList[FileHandle]
      try {
        val entries = parent.entries
        while (entries hasMoreElements) {
          val e = entries.nextElement
          if (e.getName.split("/").dropRight(1).equals(entry getName))
            result.add( new ZipEntryFileHandle(e, parent, pathTo) )
        }
       result
      } catch {
        // Don't close my ZipFile while I'm getting its' entries! Geez!
        case e: IllegalStateException => throw new IOException ("Could not list ZipFile entries, file " + path + " appears to have been closed.", e)
      }
    } else Collections.emptyList()
  }

  def write(append: Boolean) = null
}

class JarFileHandle (pathTo: String) extends FileHandle {
  // I also hate java.util.jar
  private val file = new File(pathTo)
  private val jarfile = new JarFile(file)

  def path = file getPath
  def exists: Boolean = file exists
  def isDirectory: Boolean = true
  def writeable = false

  @throws(classOf[IOException])
  def list: List[FileHandle] = {
    var result = new ArrayList[FileHandle]
    try {
      val entries = jarfile.entries

     while (entries hasMoreElements) result.add( new JarEntryFileHandle(entries.nextElement(), jarfile, path) )
     result
    } catch {
      case e: IllegalStateException => throw new IOException ("Could not list JarFile entries, file " + path + " appears to have been closed.", e)
    }
  }

  @throws(classOf[IOException])
  def list(suffix: String): List[FileHandle] = list.filter(entry => entry.path.endsWith(suffix))
  def write(append: Boolean) = null
}

class JarEntryFileHandle (private val entry: JarEntry, private val parent: JarFile, private val pathTo: String) extends FileHandle {
  def writeable = false
  def exists = true
  def isDirectory = entry isDirectory
  def path = pathTo + entry.getName//TODO: coerce paths into Unix paths.

  def read: InputStream = {
    if (!exists) throw new IOException("Could not read file:" + path + ", the requested file does not exist.")
    else if (isDirectory) throw new IOException("Could not read file:" + path + ", the requested file is a directory.")
    else parent.getInputStream(entry)
  }

  def list: List[FileHandle] = {
    if (isDirectory) {
      var result = new ArrayList[FileHandle]
      try {
        val entries = parent.entries
        while (entries hasMoreElements) {
          val e = entries.nextElement
          if (e.getName.split("/").dropRight(1).equals(entry getName))
            result.add( new JarEntryFileHandle(e, parent, pathTo) )
        }
       result
      } catch {
        case e: IllegalStateException => throw new IOException ("Could not list JarFile entries, file " + path + " appears to have been closed.", e)
      }
    } else Collections.emptyList()
  }

  def write(append: Boolean) = null
}

class AndroidFileHandle (pathTo: String, inStream: InputStream, outStream: OutputStream) extends FileHandle {
  def exists(): Boolean = ??? //TODO: Implement
  def isDirectory(): Boolean = ??? //TODO: Implement
  def list(suffix: String): List[FileHandle] = ??? //TODO: Implement
  def list: List[FileHandle] = ??? //TODO: Implement
}


object FileHandle {
  val platform = System getProperties

  def apply(path: String): FileHandle = platform.getProperty("os.name") match {
    case "Linux" => platform.getProperty("java.vendor") match {
      case "The Android Project" => new AndroidFileHandle(path, null, null)
      case _ => new DesktopFileHandle(path, null, null) // TODO: make sure the initial path is a directory
    }
    case "MacOSX" => new DesktopFileHandle(path, null, null) // TODO: make sure the initial path is a directory
    // TODO: Special-case for Windows
  }
}