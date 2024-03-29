package com.lyd.absolverdatabase.ui.page

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.lyd.absolverdatabase.App
import com.lyd.absolverdatabase.R
import com.lyd.absolverdatabase.bridge.data.bean.Deck
import com.lyd.absolverdatabase.bridge.data.bean.EditToSelectMsg
import com.lyd.absolverdatabase.bridge.data.bean.MoveBox
import com.lyd.absolverdatabase.bridge.data.repository.SettingRepository
import com.lyd.absolverdatabase.bridge.state.DeckEditState
import com.lyd.absolverdatabase.bridge.state.DeckEditViewModelFactory
import com.lyd.absolverdatabase.databinding.FragmentDeckEditBinding
import com.lyd.absolverdatabase.ui.base.BaseFragment
import com.lyd.absolverdatabase.ui.widgets.BaseDialogBuilder
import com.lyd.absolverdatabase.ui.widgets.DeckDetailDialog
import com.lyd.absolverdatabase.utils.DeckGenerate
import com.lyd.absolverdatabase.utils.TimeUtils
import com.lyd.absolverdatabase.utils.vibrate

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DeckEditFragment :BaseFragment() {

    private val editState : DeckEditState by navGraphViewModels(navGraphId = R.id.nav_deck, factoryProducer = {
        DeckEditViewModelFactory((mActivity?.application as App).deckEditRepository)
    })

    private val args :DeckEditFragmentArgs by navArgs()

    private val howToEditDeckMsg by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
        BaseDialogBuilder(requireContext())
            .setTitle(getString(R.string.how_to_edit_deckMsg_title))
            .setMessage(getString(R.string.how_To_edit_deckMsg_msg))
    }
    private val deckDetailBottomSheetDialog :DeckDetailDialog by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
        DeckDetailDialog(requireActivity())
    }

    private var dataBinding :FragmentDeckEditBinding ?= null

    private lateinit var _deckForEdit :Deck /*= DeckGenerate.generateEmptyDeck()*/


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_deck_edit,container,false)

        dataBinding = FragmentDeckEditBinding.bind(view)
        dataBinding?.lifecycleOwner = viewLifecycleOwner

        llog.d(TAG, "onCreateView: ${args.deckForEdit}")

        dataBinding?.apply {
            deckEditBarUpperRight.initClick(clickProxy = { view: View,clickWhatMove :Int ->
                beforeMoveToSelect(EditToSelectMsg.SEQ_UPPER_RIGHT,clickWhatMove)
            }, longClickProxy = { view: View,clickWhatMove :Int ->
                onLongClickBar(EditToSelectMsg.SEQ_UPPER_RIGHT, clickWhatMove = clickWhatMove)
            })
            deckEditBarUpperLeft.initClick(clickProxy = { _: View, clickWhatMove :Int ->
                beforeMoveToSelect(EditToSelectMsg.SEQ_UPPER_LEFT, clickWhatMove)
            },longClickProxy = { _: View,clickWhatMove :Int ->
                onLongClickBar(EditToSelectMsg.SEQ_UPPER_LEFT, clickWhatMove = clickWhatMove)
            })
            deckEditBarLowerLeft.initClick(clickProxy = { _: View, clickWhatMove :Int ->
                beforeMoveToSelect(EditToSelectMsg.SEQ_LOWER_LEFT, clickWhatMove)
            },longClickProxy = { _: View,clickWhatMove :Int ->
                onLongClickBar(EditToSelectMsg.SEQ_LOWER_LEFT, clickWhatMove = clickWhatMove)
            })
            deckEditBarLowerRight.initClick(clickProxy = { _: View, clickWhatMove :Int ->
                beforeMoveToSelect(EditToSelectMsg.SEQ_LOWER_RIGHT, clickWhatMove)
            },longClickProxy = { _: View,clickWhatMove :Int ->
                onLongClickBar(EditToSelectMsg.SEQ_LOWER_RIGHT, clickWhatMove = clickWhatMove)
            })


            deckEditOptionalUpperRight.initClick(clickProxy = {view: View ->
                beforeMoveToSelect(EditToSelectMsg.OPT_UPPER_RIGHT, 0)
            }, longClickProxy = {
                onLongClickOneBar(EditToSelectMsg.OPT_UPPER_RIGHT)
            })
            deckEditOptionalUpperLeft.initClick(clickProxy = {
                beforeMoveToSelect(EditToSelectMsg.OPT_UPPER_LEFT, 0)
            }, longClickProxy = {
                onLongClickOneBar(EditToSelectMsg.OPT_UPPER_LEFT)
            })
            deckEditOptionalLowerLeft.initClick(clickProxy = {
                beforeMoveToSelect(EditToSelectMsg.OPT_LOWER_LEFT, 0)
            }, longClickProxy = {
                onLongClickOneBar(EditToSelectMsg.OPT_LOWER_LEFT)
            })
            deckEditOptionalLowerRight.initClick(clickProxy = {
                beforeMoveToSelect(EditToSelectMsg.OPT_LOWER_RIGHT, 0)
            }, longClickProxy = {
                onLongClickOneBar(EditToSelectMsg.OPT_LOWER_RIGHT)
            })

            deckEditeFabSave.setOnClickListener { view ->
                editState.saveDeckInSaved(_deckForEdit.copy(updateTime = TimeUtils.curTime),
                    isForSave = true,
                    ifError = {
                        llog.e(TAG, "saveDeckInSavedError: $it")
                        showShortToast(it)
                    },
                    ifSuccess = {
                        llog.i(TAG, "saveDeckInSavedSuccess: $it")
                        showShortToast(getString(R.string.save_deck_success,it))
                    })
            }

        }

        dataBinding?.apply {
            deckEditConstrainBg?.setOnLongClickListener {view ->
                // 这一步，我把这个_deckForEdit中的deck传进了dialog，然后里面的修改也会改变外面的值，注意不要用args.deckForEdit这个数据，对象不一样
                deckDetailBottomSheetDialog.apply { mDeck = _deckForEdit }.show()
                return@setOnLongClickListener true
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (SettingRepository.hadShowTipHowToEditDeckMsg){
            lifecycleScope.launchWhenResumed {
                SettingRepository.hadShowTipHowToEditDeckMsg = false
                SettingRepository.hadShowTipHowToEditDeckMsgPreference.set { false }
                howToEditDeckMsg.show()
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.sequenceUpperRight.collectLatest {
                    dataBinding?.apply {
                        deckEditMsgsBarUpperRight.updateMsg(it)
                        deckEditBarUpperRight.updateMoves(it)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.sequenceUpperLeft.collectLatest {
                    dataBinding?.apply {
                        deckEditBarUpperLeft.updateMoves(it)
                        deckEditMsgsBarUpperLeft.updateMsg(it)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.sequenceLowerLeft.collectLatest {
                    dataBinding?.apply {
                        deckEditBarLowerLeft.updateMoves(it)
                        deckEditMsgsBarLowerLeft.updateMsg(it)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.sequenceLowerRight.collectLatest {
                    dataBinding?.apply {
                        deckEditBarLowerRight.updateMoves(it)
                        deckEditMsgsBarLowerRight.updateMsg(it)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.optUpperRight.collectLatest {
                    dataBinding?.apply {
                        deckEditOptionalUpperRight.updateMove(it)
                        deckEditOptionalMsgUpperRight.updateOneMsg(it)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.optUpperLeft.collectLatest {
                    dataBinding?.apply {
                        deckEditOptionalUpperLeft.updateMove(it)
                        deckEditOptionalMsgUpperLeft.updateOneMsg(it)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.optLowerLift.collectLatest {
                    dataBinding?.apply {
                        deckEditOptionalLowerLeft.updateMove(it)
                        deckEditOptionalMsgLowerLeft.updateOneMsg(it)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.optLowerRight.collectLatest {
                    dataBinding?.apply {
                        deckEditOptionalLowerRight.updateMove(it)
                        deckEditOptionalMsgLowerRight.updateOneMsg(it)
                    }
                }
            }
        }

        // 注意，这个必须放在最后，免得发射的时间比建立监听的时间还快
        lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                editState.deckInSaved.collectLatest { deckInSave ->
                    // 这里能够获取到最新的编辑卡组的数据
                    // 不论怎么样，都是需要变更布局的
                    if (deckInSave == DeckGenerate.generateEmptyDeck(isFromDeckToEdit = true)){
                        // 可以判断为是空卡组，是从deckFragment跳转进来的
                        _deckForEdit = args.deckForEdit
                        llog.i(TAG, "deckInSaved: 从deckFragment跳转进来的，进行数据存储，然后返回")
                        editState.saveDeckInSaved(_deckForEdit.also { if (it.updateTime == 1L) it.updateTime = 0L})
                        return@collectLatest
                    } else {
                        // 不相等，是从其他界面切回来的，因为假如是从deckFragment跳转，则会将其设置为空卡组且isFromDeckToEdit = true
                        _deckForEdit = deckInSave
                        llog.i(TAG, "deckInSaved: 不相等，是从其他界面切回来的")
                    }
                    // 这里应该触发招式序列list的变化，进行初始化，让bar自己判断需不需要变更bg
                    editState.updateAllSequence(_deckForEdit)
                    editState.updateAllOption(_deckForEdit)
                }
            }
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        // dataBinding要释放掉
        dataBinding = null
    }

    private fun beforeMoveToSelect(@IntRange(0, 7) whatForEdit: Int,@IntRange(0,2) clickWhatMove: Int){
        editState.initFilterOption()
        editState.selectWhatMoveInSeq(clickWhatMove)
        editState.setWhatBarToEdit(whatForEdit)
        nav().navigate(DeckEditFragmentDirections.actionDeckEditFragmentToMoveSelectFragment())
    }

    private fun onLongClickBar(@IntRange(0, 3) whatForEdit: Int,@IntRange(0,2) clickWhatMove: Int){
        when(whatForEdit){
            0 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    sequenceUpperRight[clickWhatMove] = MoveBox()
                })
                lifecycleScope.launchWhenStarted {
                    editState.updateSeqUpperRight(editState.getDeckInSaved()!!.sequenceUpperRight)
                }
            }
            1 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    sequenceUpperLeft[clickWhatMove] = MoveBox()
                })
                lifecycleScope.launchWhenStarted {
                    editState.updateSeqUpperLeft(editState.getDeckInSaved()!!.sequenceUpperLeft)
                }
            }
            2 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    sequenceLowerLeft[clickWhatMove] = MoveBox()
                })
                lifecycleScope.launchWhenStarted {
                    editState.updateSeqLowerLeft(editState.getDeckInSaved()!!.sequenceLowerLeft)
                }
            }
            3 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    sequenceLowerRight[clickWhatMove] = MoveBox()
                })
                lifecycleScope.launchWhenStarted {
                    editState.updateSeqLowerRight(editState.getDeckInSaved()!!.sequenceLowerRight)
                }
            }
        }
        requireContext().vibrate()
    }
    private fun onLongClickOneBar(@IntRange(4,7) whatForEdit: Int){
        when(whatForEdit){
            4 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    optionalUpperRight = MoveBox()
                })
                lifecycleScope.launchWhenStarted { editState.optUpperRight.emit(MoveBox()) }
            }
            5 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    optionalUpperLeft = MoveBox()
                })
                lifecycleScope.launchWhenStarted { editState.optUpperLeft.emit(MoveBox()) }
            }
            6 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    optionalLowerLeft = MoveBox()
                })
                lifecycleScope.launchWhenStarted { editState.optLowerLift.emit(MoveBox()) }
            }
            7 ->{
                editState.saveDeckInSaved(editState.getDeckInSaved()!!.apply {
                    optionalLowerRight = MoveBox()
                })
                lifecycleScope.launchWhenStarted { editState.optLowerRight.emit(MoveBox()) }
            }
        }
        requireContext().vibrate()
    }
}