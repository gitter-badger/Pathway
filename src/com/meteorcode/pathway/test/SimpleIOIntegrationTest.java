package com.meteorcode.pathway.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.meteorcode.pathway.io.*;
import java.io.IOException;

/**
 * Non-comprehensive test case to assert that the IO package does the Right Thing
 * THIS IS NOT A UNIT TEST - io classes should be unit tested for coverage as well.
 */
public class SimpleIOIntegrationTest {
    private FileHandle underTest;

	@Test
	public void testUnzippedFileHandle() throws IOException {
        underTest = FileHandle.handle("testAssets/test1.txt");
        assertEquals("hi!", underTest.readString());
	}

    @Test
    public void testZippedFileHandle() throws IOException {
        underTest = FileHandle.handle("testAssets/zippedtest.zip");
        assertTrue(underTest.isDirectory());
        assertEquals(underTest.list().get(0).readString(), "also hi!");
    }

    @Test
    public void testDirFileHandle() throws IOException{
        underTest = FileHandle.handle("testAssets/testDir");
        assertTrue(underTest.isDirectory());
        assertEquals("yet again hi", underTest.list().get(0).readString());
        assertEquals("still hi", underTest.list().get(1).readString());
    }

}
