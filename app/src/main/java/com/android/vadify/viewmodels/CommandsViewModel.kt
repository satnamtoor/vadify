package com.android.vadify.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.android.vadify.data.api.models.CommandsRequestModel
import com.android.vadify.data.db.DbModule
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.commands.CommandDao
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.ui.login.fragment.CommandsSpeech
import com.android.vadify.utils.BaseViewModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.uiview.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


class CommandsViewModel @Inject constructor(application: Application) : BaseViewModel() {

    var data: MutableLiveData<List<Commands>> = MutableLiveData()
    //  var result: MutableLiveData<List<Commands>> = MutableLiveData()

    private val commandDao: CommandDao = DbModule().provideAppCache(application).commandsDao()

    fun insert(command: Commands) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.insert(command)
        }
    }

    fun update(command: Commands) {
        CoroutineScope(Dispatchers.IO).launch {
            commandDao.update(
                command.command1,
                command.command2,
                command.command3,
                command.language,
                command.commandName
            )

        }
    }


    fun getCommand(command: String, lanuage: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result: List<Commands> = commandDao.getAll(command, lanuage)
            viewModelScope.launch {
                data.value = result
            }

        }

    }
   /* fun getCount() {
        CoroutineScope(Dispatchers.IO).launch {
            val result: List<Commands> = commandDao.getCount()
            viewModelScope.launch {
                data.value = result
            }
            Log.d("TAGInserted", Gson().toJson(result))
        }

    }*/
    fun getCommandAll(lanuage: String) {

        CoroutineScope(Dispatchers.IO).launch {
            Log.d("all-commands",lanuage)
            val result: List<Commands> = commandDao.getAllChat(lanuage)
            viewModelScope.launch {
                data.value = result

            }
            //Log.d("all-commands", Gson().toJson(result))
        }

    }


}