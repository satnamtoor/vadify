package com.android.vadify.di

import androidx.lifecycle.ViewModel
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.android.vadify.ui.chat.contact.viewmodel.UserContactViewModel
import com.android.vadify.ui.chat.viewmodel.CameraViewModel
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.chat.viewmodel.CreateGroupModel
import com.android.vadify.ui.chat.viewmodel.ReplyMessageViewModel
import com.android.vadify.ui.contact.viewmodel.ContactViewModel
import com.android.vadify.ui.dashboard.viewmodel.*
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.viewmodels.CommandsViewModel
import com.android.vadify.viewmodels.EncryptionViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(LoginFragmentViewModel::class)
    abstract fun bindLoginFragmentViewModel(
        viewModel: LoginFragmentViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileFragmentViewModel::class)
    abstract fun bindProfileFragmentViewModel(
        viewModel: ProfileFragmentViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(CreateGroupModel::class)
    abstract fun bindCreateGroupViewModel(
        viewModel: CreateGroupModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingFragmentViewModel::class)
    abstract fun bindSettingFragmentViewModel(
        viewModel: SettingFragmentViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BlockedContentViewModel::class)
    abstract fun bindBlockedContentViewModel(
        viewModel: BlockedContentViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(EditProfileViewModel::class)
    abstract fun bindEditProfileViewModel(
        viewModel: EditProfileViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(EditMyAccountViewModel::class)
    abstract fun bindEditMyAccountViewModel(
        viewModel: EditMyAccountViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationViewModel::class)
    abstract fun bindNotificationViewModel(
        viewModel: NotificationViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EditChatViewModel::class)
    abstract fun bindEditChatViewModel(
        viewModel: EditChatViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(TermPolicyViewModel::class)
    abstract fun bindTermPolicyViewModel(
        viewModel: TermPolicyViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(DashBoardViewModel::class)
    abstract fun bindDashBoardViewModel(
        viewModel: DashBoardViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(EncryptionViewModel::class)
    abstract fun bindEncryptionViewModel(
        viewModel: EncryptionViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(VadifyFriendViewModel::class)
    abstract fun bindVadifyFriendViewModel(
        viewModel: VadifyFriendViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(ContactViewModel::class)
    abstract fun bindContactViewModel(
        viewModel: ContactViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(ChatViewModel::class)
    abstract fun bindChatViewModel(
        viewModel: ChatViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(UserListViewModel::class)
    abstract fun bindUserListViewModel(
        viewModel: UserListViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ReplyMessageViewModel::class)
    abstract fun bindReplyMessageViewModel(
        viewModel: ReplyMessageViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(CameraViewModel::class)
    abstract fun bindCameraViewModel(
        viewModel: CameraViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(UserContactViewModel::class)
    abstract fun bindUserContactViewModel(
        viewModel: UserContactViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(CallViewModel::class)
    abstract fun bindCallViewModel(
        viewModel: CallViewModel
    ): ViewModel


    @Binds
    @IntoMap
    @ViewModelKey(CallLogViewModel::class)
    abstract fun bindCallLogViewModel(
        viewModel: CallLogViewModel
    ): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CommandsViewModel::class)
    abstract fun bindCommandsViewModel(
        viewModel: CommandsViewModel
    ): ViewModel

}
