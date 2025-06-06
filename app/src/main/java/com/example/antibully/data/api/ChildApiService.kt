package com.example.antibully.data.api

import com.example.antibully.data.models.ChildResponse
import com.example.antibully.data.models.LinkChildRequest
import com.example.antibully.data.models.UpdateChildRequest
import com.example.antibully.data.models.UnlinkChildResponse
import retrofit2.Response
import retrofit2.http.*

interface ChildApiService {
    
    @POST("api/child/parents/{parentId}/children") // ✅ Add 'api/' prefix
    suspend fun linkChild(
        @Header("Authorization") token: String,
        @Path("parentId") parentId: String,
        @Body childData: LinkChildRequest
    ): Response<ChildResponse>
    
    @GET("api/child/parents/{parentId}/children") // ✅ Add 'api/' prefix
    suspend fun getChildrenForParent(
        @Header("Authorization") token: String,
        @Path("parentId") parentId: String
    ): Response<List<ChildResponse>>
    
    @PUT("api/child/parents/{parentId}/children/{discordId}") // ✅ Add 'api/' prefix
    suspend fun updateChild(
        @Header("Authorization") token: String,
        @Path("parentId") parentId: String,
        @Path("discordId") discordId: String,
        @Body updateData: UpdateChildRequest
    ): Response<ChildResponse>
    
    @DELETE("api/child/parents/{parentId}/children/{discordId}") // ✅ Add 'api/' prefix
    suspend fun unlinkChild(
        @Header("Authorization") token: String,
        @Path("parentId") parentId: String,
        @Path("discordId") discordId: String
    ): Response<UnlinkChildResponse>
}