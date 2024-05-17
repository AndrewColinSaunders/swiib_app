package com.example.warehousetet

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doNothing
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.any

class OdooXmlRpcClientTest {

    @Mock
    lateinit var xmlRpcClient: XmlRpcClient  // Mock the XML-RPC client
    @Mock
    lateinit var credentialManager: CredentialManager

    lateinit var odooXmlRpcClient: OdooXmlRpcClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        odooXmlRpcClient = OdooXmlRpcClient(credentialManager, xmlRpcClient)

        val config = XmlRpcClientConfigImpl()
        doNothing().`when`(xmlRpcClient).setConfig(any())

        // Set up successful authentication with specific credentials
        `when`(
            xmlRpcClient.execute(
                "authenticate",
                arrayOf(Constants.DATABASE, "1", "1", emptyMap<String, Any>())
            )
        ).thenReturn(6)  // User ID 6 for successful authentication

        // Set up failed authentication with incorrect credentials
        `when`(
            xmlRpcClient.execute(
                "authenticate",
                arrayOf(Constants.DATABASE, "wronguser", "wrongpass", emptyMap<String, Any>())
            )
        ).thenReturn(-1)  // Simulating failed authentication
    }

    @Test
    fun login_successful() = runBlocking {
        val userId = odooXmlRpcClient.login("1", "1")
        assertEquals("Expected user ID did not match", 6, userId)
        verify(credentialManager).storeUserCredentials("1", "1", 6)
    }

    @Test
    fun login_failure() = runBlocking {
        val userId = odooXmlRpcClient.login("wronguser", "wrongpass")
        assertEquals("Expected failure did not match", -1, userId)
        verify(credentialManager, never()).storeUserCredentials(any(), any(), any())
    }
}
