package app.suhasdissa.foode.backend.viewmodels

import android.content.ClipboardManager
import android.content.SharedPreferences
import android.util.Log
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
import app.suhasdissa.foode.backend.database.entities.AdditivesEntity
import app.suhasdissa.foode.backend.repositories.AdditivesRepository
import app.suhasdissa.foode.backend.repositories.TranslationRepository
import app.suhasdissa.foode.backend.viewmodels.states.TranslationState
import app.suhasdissa.foode.utils.autoTranslateKey
import app.suhasdissa.foode.utils.preferences
import java.util.Locale
import kotlinx.coroutines.launch

class AdditiveDetailViewModel(
    private val additivesRepository: AdditivesRepository,
    private val clipboard: ClipboardManager,
    private val transtationRepository: TranslationRepository,
    private val preferences: SharedPreferences
) : ViewModel() {
    var additive: AdditivesEntity? by mutableStateOf(null)
        private set
    var translationState: TranslationState by mutableStateOf(TranslationState.NotTranslated)
        private set

    private val languageCode: String = Locale.getDefault().language
    private var supportedLanguages = listOf<String>()

    private fun getTranslation(title: String, description: String) {
        val autoTranslate = preferences.getBoolean(autoTranslateKey, false)
        if (!autoTranslate || languageCode == "en") {
            translationState = TranslationState.NotTranslated
            return
        }
        viewModelScope.launch {
            translationState = TranslationState.Loading
            if (supportedLanguages.isEmpty()) {
                try {
                    supportedLanguages = transtationRepository.getLanguages().map { it.code }
                } catch (e: Exception) {
                    Log.e("Translate Error", e.toString())
                    translationState = TranslationState.Error
                    return@launch
                }
            }
            if (!supportedLanguages.contains(languageCode)) {
                translationState = TranslationState.NotSupported
                return@launch
            }
            translationState = try {
                val titleTrans = transtationRepository.getTranslation(languageCode, title)
                val descriptionTrans =
                    transtationRepository.getTranslation(languageCode, description)
                additive = additive?.copy(info = descriptionTrans, title = titleTrans)
                TranslationState.Success
            } catch (e: Exception) {
                Log.e("Translate Error", e.toString())
                TranslationState.Error
            }
        }
    }

    fun getClipboard(): ClipboardManager {
        return clipboard
    }

    fun getAdditive(id: Int) {
        viewModelScope.launch {
            additive = additivesRepository.getAdditive(id)
            additive?.let { additive ->
                getTranslation(additive.title, additive.info)
            }
        }
    }

    fun setFavourite(favourite: Int) {
        viewModelScope.launch {
            additivesRepository.setFavourite(additive!!.id, favourite)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as FoodeApplication)
                AdditiveDetailViewModel(
                    additivesRepository = application.container.additivesRepository,
                    clipboard = application.container.clipboardManager,
                    transtationRepository = application.container.translationRepository,
                    preferences = application.preferences
                )
            }
        }
    }
}
