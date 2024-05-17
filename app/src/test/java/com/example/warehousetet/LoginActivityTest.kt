//package com.example.warehousetet
//
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.test.runBlockingTest
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.mockito.Mock
//import org.mockito.Mockito.*
//import org.mockito.junit.MockitoJUnitRunner
//import android.content.Context
//
//@RunWith(MockitoJUnitRunner::class)
//@ExperimentalCoroutinesApi
//class LoginActivityTest {
//
//    @get:Rule
//    var instantExecutorRule = InstantTaskExecutorRule()
//
//    @Mock
//    private lateinit var mockContext: Context
//    @Mock
//    private lateinit var credentialManager: CredentialManager
//    @Mock
//    private lateinit var odooXmlRpcClient: OdooXmlRpcClient
//
//    private lateinit var loginActivity: LoginActivity
//
//    @Before
//    fun setup() {
//        loginActivity = LoginActivity()
//        loginActivity.credentialManager = credentialManager
//        loginActivity.odooXmlRpcClient = odooXmlRpcClient
//    }
//
//    @Test
//    fun login_Successful() = runBlockingTest {
//        val username = "test"
//        val password = "password"
//        val userId = 1
//
//        `when`(odooXmlRpcClient.login(username, password)).thenReturn(userId)
//
//        loginActivity.performLogin(username, password)
//
//        verify(credentialManager).storeUserCredentials(username, password, userId)
//        verify(odooXmlRpcClient).login(username, password)
//        verify(loginActivity).handleLoginSuccess(username, password, userId)
//    }
//
//    @Test
//    fun login_Failed() = runBlockingTest {
//        val username = "wrong"
//        val password = "wrong"
//        val userId = -1
//
//        `when`(odooXmlRpcClient.login(username, password)).thenReturn(userId)
//
//        loginActivity.performLogin(username, password)
//
//        verify(credentialManager, never()).storeUserCredentials(anyString(), anyString(), anyInt())
//        verify(odooXmlRpcClient).login(username, password)
//        verify(loginActivity).handleLoginFailure("Invalid username or password.")
//    }
//}
