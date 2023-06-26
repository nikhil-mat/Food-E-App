package app.suhasdissa.foode.backend.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.suhasdissa.foode.FoodeApplication
import app.suhasdissa.foode.backend.database.entities.BarcodeEntity
import app.suhasdissa.foode.backend.models.Product
import app.suhasdissa.foode.backend.repositories.BarcodeHistoryRepository
import app.suhasdissa.foode.backend.repositories.OpenFoodFactRepository
import kotlinx.coroutines.launch

sealed interface FoodFactUiState {
    data class Success(val product: Product) : FoodFactUiState
    data class Error(val error: String) : FoodFactUiState
    object ProductNotFound : FoodFactUiState
    object Loading : FoodFactUiState
}

class FoodFactsViewModel(
    private val openFoodFactRepository: OpenFoodFactRepository,
    private val barcodeHistoryRepository: BarcodeHistoryRepository
) : ViewModel() {

    var foodFactUiState: FoodFactUiState by mutableStateOf(FoodFactUiState.Loading)

    fun getProduct(barcode: String) {
        viewModelScope.launch {
            foodFactUiState = FoodFactUiState.Loading
            foodFactUiState = try {
                val product = openFoodFactRepository.getOnlineData(barcode)
                product?.let {
                    it.code?.let { code ->
                        addBarcode(
                            BarcodeEntity(
                                code, it.productName ?: "Unknown Product", it.imageUrl ?: ""
                            )
                        )
                    }
                    FoodFactUiState.Success(it)
                } ?: FoodFactUiState.ProductNotFound
            } catch (_: retrofit2.HttpException) {
                FoodFactUiState.ProductNotFound
            } catch (e: Exception) {
                FoodFactUiState.Error(e.toString())
            }
        }
    }

    private fun addBarcode(barcode: BarcodeEntity) {
        viewModelScope.launch {
            barcodeHistoryRepository.saveBarcode(barcode)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FoodeApplication)
                FoodFactsViewModel(
                    application.container.openFoodFactRepository,
                    application.container.barcodeHistoryRepository
                )
            }
        }
    }
}