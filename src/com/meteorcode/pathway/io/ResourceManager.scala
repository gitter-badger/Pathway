package com.meteorcode.pathway.io

class ResourceManager (private val assetsDir: FileHandle) {
  def this() = this(FileHandle("assets"))
  // TODO: traverse the tree from the initial FileHandle down and call list(), building the tree?
}