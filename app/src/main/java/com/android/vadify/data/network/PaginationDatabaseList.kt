package com.android.vadify.data.network


abstract class PaginationDatabaseList<LocalType, RemoteType>(appExecutors: AppExecutors) :
    NetworkListResourceBoundaryCallback<LocalType, RemoteType>(appExecutors)