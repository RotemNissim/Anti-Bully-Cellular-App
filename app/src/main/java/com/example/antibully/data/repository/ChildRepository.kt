package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.ChildApiService
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.dao.ChildDao
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.models.LinkChildRequest
import com.example.antibully.data.models.UpdateChildRequest
import kotlinx.coroutines.flow.Flow

class ChildRepository(
    private val childDao: ChildDao,
    private val childApiService: ChildApiService = RetrofitClient.childApiService
) {
    
    fun getChildrenForUser(userId: String): Flow<List<ChildLocalData>> = 
        childDao.getChildrenForUserFlow(userId)
    
    suspend fun fetchChildrenFromApi(token: String, parentId: String) {
        Log.d("ChildRepository", "Fetching children for parent: $parentId")
        val bearer = "Bearer $token"
        
        val result = ApiHelper.safeApiCall {
            childApiService.getChildrenForParent(bearer, parentId)
        }
        
        if (result.isSuccess) {
            val remoteChildren = result.getOrNull() ?: emptyList()
            Log.d("ChildRepository", "Received ${remoteChildren.size} children from API")
            
            val localChildren = remoteChildren.map { apiChild ->
                Log.d("ChildRepository", "Mapping child: ${apiChild.discordId}, name: ${apiChild.name}")
                ChildLocalData(
                    childId = apiChild.discordId,
                    parentUserId = parentId,
                    name = apiChild.name ?: apiChild.discordId,
                    imageUrl = apiChild.imageUrl
                )
            }
            childDao.replaceChildrenForUser(parentId, localChildren)
            Log.d("ChildRepository", "Saved ${localChildren.size} children to local database")
        } else {
            Log.e("ChildRepository", "Failed to fetch children: ${result.exceptionOrNull()?.message}")
            result.exceptionOrNull()?.printStackTrace()
        }
    }
    
    suspend fun linkChild(token: String, parentId: String, discordId: String, name: String, imageUrl: String? = null): Boolean {
        Log.d("ChildRepository", "Linking child: $discordId to parent: $parentId")
        val bearer = "Bearer $token"
        val request = LinkChildRequest(discordId, name, imageUrl)
        
        val result = ApiHelper.safeApiCall {
            childApiService.linkChild(bearer, parentId, request)
        }
        
        if (result.isSuccess) {
            val apiChild = result.getOrNull()
            if (apiChild != null) {
                Log.d("ChildRepository", "Successfully linked child: ${apiChild.discordId}")
                val localChild = ChildLocalData(
                    childId = apiChild.discordId,
                    parentUserId = parentId,
                    name = apiChild.name ?: name,
                    imageUrl = apiChild.imageUrl
                )
                childDao.insertChild(localChild)
                return true
            }
        } else {
            Log.e("ChildRepository", "Failed to link child: ${result.exceptionOrNull()?.message}")
        }
        return false
    }
    
    suspend fun updateChild(token: String, parentId: String, discordId: String, name: String?, imageUrl: String?): Boolean {
        val bearer = "Bearer $token"
        val request = UpdateChildRequest(name, imageUrl)
        
        val result = ApiHelper.safeApiCall {
            childApiService.updateChild(bearer, parentId, discordId, request)
        }
        
        if (result.isSuccess) {
            val apiChild = result.getOrNull()
            if (apiChild != null) {
                val localChild = ChildLocalData(
                    childId = apiChild.discordId,
                    parentUserId = parentId,
                    name = apiChild.name ?: name ?: "",
                    imageUrl = apiChild.imageUrl
                )
                childDao.insertChild(localChild)
                return true
            }
        }
        return false
    }
    
    suspend fun unlinkChild(token: String, parentId: String, discordId: String): Boolean {
        val bearer = "Bearer $token"
        
        val result = ApiHelper.safeApiCall {
            childApiService.unlinkChild(bearer, parentId, discordId)
        }
        
        if (result.isSuccess) {
            childDao.deleteChild(discordId, parentId)
            return true
        }
        return false
    }
    
    // Local operations for offline support
    suspend fun insertChildLocally(child: ChildLocalData) {
        childDao.insertChild(child)
    }
    
    suspend fun updateChildLocally(childId: String, parentId: String, name: String, imageUrl: String) {
        childDao.updateChild(childId, parentId, name, imageUrl)
    }
}