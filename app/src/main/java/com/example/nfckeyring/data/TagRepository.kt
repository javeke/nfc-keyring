package com.example.nfckeyring.data

import kotlinx.coroutines.flow.Flow

class TagRepository(private val tagDao: TagDao) {
    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun insert(tag: TagEntity) = tagDao.insert(tag)

    suspend fun update(tag: TagEntity) = tagDao.update(tag)

    suspend fun delete(tag: TagEntity) = tagDao.delete(tag)

    suspend fun getById(id: Int): TagEntity? = tagDao.getById(id)
}
