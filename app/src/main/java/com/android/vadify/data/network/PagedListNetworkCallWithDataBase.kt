package com.android.vadify.data.network

import androidx.paging.DataSource

open class PagedListNetworkCallWithDataBase<LocalType>(
    dataSourceFactory: DataSource.Factory<Int, LocalType>,
    paginationNetworkResource: PageListNetworkResource<LocalType>
) : PagedListNetworkResourceDatabase<LocalType, Int>(dataSourceFactory, paginationNetworkResource)