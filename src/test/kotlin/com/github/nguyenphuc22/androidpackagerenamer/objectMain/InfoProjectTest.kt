package com.github.nguyenphuc22.androidpackagerenamer.objectMain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class InfoProjectTest {

    @Test
    fun `should create InfoProject with required parameters`() {
        val oldPackage = "com.example.old"
        val newPackage = "com.example.new"
        
        val infoProject = InfoProject(newPackage, oldPackage)
        
        assertEquals(oldPackage, infoProject.packageNameOld)
        assertEquals(newPackage, infoProject.packageNameNew)
        assertFalse(infoProject.isDataBindingMode)
    }

    @Test
    fun `should create InfoProject with data binding mode enabled`() {
        val oldPackage = "com.example.old"
        val newPackage = "com.example.new"
        val dataMode = true
        
        val infoProject = InfoProject(newPackage, oldPackage, dataMode)
        
        assertEquals(oldPackage, infoProject.packageNameOld)
        assertEquals(newPackage, infoProject.packageNameNew)
        assertTrue(infoProject.isDataBindingMode)
    }

    @Test
    fun `should create InfoProject with data binding mode disabled explicitly`() {
        val oldPackage = "com.example.old"
        val newPackage = "com.example.new"
        val dataMode = false
        
        val infoProject = InfoProject(newPackage, oldPackage, dataMode)
        
        assertEquals(oldPackage, infoProject.packageNameOld)
        assertEquals(newPackage, infoProject.packageNameNew)
        assertFalse(infoProject.isDataBindingMode)
    }

    @Test
    fun `should handle empty package names`() {
        val oldPackage = ""
        val newPackage = ""
        
        val infoProject = InfoProject(newPackage, oldPackage)
        
        assertEquals(oldPackage, infoProject.packageNameOld)
        assertEquals(newPackage, infoProject.packageNameNew)
        assertFalse(infoProject.isDataBindingMode)
    }

    @Test
    fun `should allow modification of properties after creation`() {
        val infoProject = InfoProject("new", "old")
        
        infoProject.packageNameOld = "modified.old"
        infoProject.packageNameNew = "modified.new"
        infoProject.isDataBindingMode = true
        
        assertEquals("modified.old", infoProject.packageNameOld)
        assertEquals("modified.new", infoProject.packageNameNew)
        assertTrue(infoProject.isDataBindingMode)
    }
}