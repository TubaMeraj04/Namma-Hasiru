package com.example.nammahasiru

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nammahasiru.data.TreeDao
import com.example.nammahasiru.data.TreeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TreeViewModel(private val treeDao: TreeDao) : ViewModel() {

    val allTrees: Flow<List<TreeEntity>> = treeDao.getAllTrees()
    
    val totalTrees: StateFlow<Int> = treeDao.getTotalTreesPlanted()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
        
    val survivedTrees: StateFlow<Int> = treeDao.getSurvivedTreesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun insertTree(tree: TreeEntity) {
        viewModelScope.launch {
            treeDao.insertTree(tree)
        }
    }

    fun updateTreeStatus(tree: TreeEntity, newStatus: String) {
        viewModelScope.launch {
            treeDao.updateTree(tree.copy(status = newStatus))
        }
    }

    fun updateTree(tree: TreeEntity) {
        viewModelScope.launch {
            treeDao.updateTree(tree)
        }
    }

    suspend fun getTreeById(id: Int): TreeEntity? {
        return treeDao.getTreeById(id)
    }
}

class TreeViewModelFactory(private val treeDao: TreeDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TreeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TreeViewModel(treeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
