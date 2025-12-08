package com.buzbuz.smartautoclicker.feature.smart.config.ui.creation

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfig
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.ArgumentMatchers.anyInt

import org.robolectric.RobolectricTestRunner
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScenarioCreationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var repository: IRepository
    @Mock
    private lateinit var displayConfigManager: DisplayConfigManager
    @Mock
    private lateinit var displayConfig: DisplayConfig

    private lateinit var viewModel: ScenarioCreationViewModel
    private val testDispatcher = StandardTestDispatcher()



    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        `when`(context.getString(anyInt())).thenReturn("Default name")
        `when`(displayConfigManager.displayConfig).thenReturn(displayConfig)
        `when`(displayConfig.sizePx).thenReturn(android.graphics.Point(1080, 1920))

        viewModel = ScenarioCreationViewModel(context, repository, displayConfigManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is configuring`() = runTest {
        assertEquals(CreationState.CONFIGURING, viewModel.creationState.first())
        assertEquals("Default name", viewModel.name.first())
    }

    @Test
    fun `empty name shows error`() = runTest {
        viewModel.setName("")
        assertTrue(viewModel.nameError.first())
    }

    @Test
    fun `valid name does not show error`() = runTest {
        viewModel.setName("Valid Name")
        assertFalse(viewModel.nameError.first())
    }

    @Test
    fun `createScenario calls repository`() = runTest {
        viewModel.setName("New Scenario")
        viewModel.createScenario()
        testDispatcher.scheduler.advanceUntilIdle()

        val captor = argumentCaptor<Scenario>()
        verify(repository).addScenario(captor.capture())
        assertEquals("New Scenario", captor.firstValue.name)
        assertEquals(CreationState.SAVED, viewModel.creationState.first())
    }
}
