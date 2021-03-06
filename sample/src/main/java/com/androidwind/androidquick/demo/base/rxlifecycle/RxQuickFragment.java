package com.androidwind.androidquick.demo.base.rxlifecycle;

/**
 * @author ddnosh
 * @website http://blog.csdn.net/ddnosh
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidwind.androidquick.demo.base.mvp.BaseContract;
import com.androidwind.androidquick.module.asynchronize.eventbus.EventCenter;
import com.androidwind.androidquick.ui.base.QuickActivity;
import com.androidwind.androidquick.ui.dialog.dialogactivity.CommonDialog;
import com.androidwind.androidquick.ui.viewstatus.VaryViewHelperController;
import com.androidwind.androidquick.util.StringUtil;
import com.trello.rxlifecycle2.components.support.RxFragment;

import java.lang.reflect.Field;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;

/**
 * @author  ddnosh
 * @website http://blog.csdn.net/ddnosh
 */
public abstract class RxQuickFragment extends RxFragment implements BaseContract.BaseView {

    protected static String TAG = "QuickFragment";

    protected Context mContext = null;

    private boolean isFirstResume = true;
    private boolean isFirstVisible = true;
    private boolean isFirstInvisible = true;
    private boolean isPrepared;

    private VaryViewHelperController mVaryViewHelperController = null;

    /**
     * butterknife 8+ support
     */
    private Unbinder mUnbinder;

    /**
     * databinding
     */
    public View mainLayout;
    protected ViewDataBinding binding;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isBindEventBus()) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        int layoutId = getContentViewLayoutID();
        if (layoutId != 0) {
            try {
                binding= DataBindingUtil.inflate(inflater,layoutId, container, false);
                if (binding != null) {
                    mainLayout = binding.getRoot();
                } else {
                    mainLayout=inflater.inflate(layoutId, container, false);
                }
            }catch (NoClassDefFoundError e){
                mainLayout=inflater.inflate(layoutId, container, false);
            }
            mainLayout.setClickable(true);
            return mainLayout;
        } else {
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUnbinder = ButterKnife.bind(this, view);

        if (null != setDefaultVaryViewRoot()) {
            mVaryViewHelperController = new VaryViewHelperController(setDefaultVaryViewRoot());
        }

        initViewsAndEvents(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUnbinder != null) {
            mUnbinder.unbind();
        }
        if (isBindEventBus()) {
            EventBus.getDefault().unregister(this);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // for bug ---> java.lang.IllegalStateException: Activity has been destroyed
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initPrepare();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstResume) {
            isFirstResume = false;
            return;
        }
        if (getUserVisibleHint()) {
            onUserVisible();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getUserVisibleHint()) {
            onUserInvisible();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (isFirstVisible) {
                isFirstVisible = false;
                initPrepare();
            } else {
                onUserVisible();
            }
        } else {
            if (isFirstInvisible) {
                isFirstInvisible = false;
                onFirstUserInvisible();
            } else {
                onUserInvisible();
            }
        }
    }

    private synchronized void initPrepare() {
        if (isPrepared) {
            onFirstUserVisible();
        } else {
            isPrepared = true;
        }
    }

    /**
     * bind eventbus
     *
     * @return
     */
    protected abstract boolean isBindEventBus();
    /**
     * when fragment is visible for the first time, here we can do some initialized work or refresh data only once
     */
    protected abstract void onFirstUserVisible();

    /**
     * this method like the fragment's lifecycle method onResume()
     */
    protected abstract void onUserVisible();

    /**
     * when fragment is invisible for the first time
     */
    private void onFirstUserInvisible() {
        // here we do not recommend do something
    }

    /**
     * this method like the fragment's lifecycle method onPause()
     */
    protected abstract void onUserInvisible();

    /**
     * get loading target view
     */
    protected abstract View setDefaultVaryViewRoot();

    /**
     * init all views and add events
     */
    protected abstract void initViewsAndEvents(Bundle savedInstanceState);

    /**
     * bind layout resource file
     *
     * @return id of layout resource
     */
    protected abstract int getContentViewLayoutID();

    /**
     * when event comming
     *
     * @param eventCenter
     */
    protected abstract void onEventComing(EventCenter eventCenter);

    /**
     * get the support fragment manager
     *
     * @return
     */
    protected FragmentManager getSupportFragmentManager() {
        return getActivity().getSupportFragmentManager();
    }

    protected Intent getGoIntent(Class<?> clazz) {
        return new Intent(getActivity(), clazz);
    }

    /**
     * startActivity
     *
     * @param clazz
     */
    protected void readyGo(Class<?> clazz) {
        Intent intent = getGoIntent(clazz);
        startActivity(intent);
    }

    /**
     * startActivity with bundle
     *
     * @param clazz
     * @param bundle
     */
    protected void readyGo(Class<?> clazz, Bundle bundle) {
        Intent intent = getGoIntent(clazz);
        if (null != bundle) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * startActivityForResult
     *
     * @param clazz         类名.class，获取类型类
     * @param requestCode   请求码
     */
    protected void readyGoForResult(Class<?> clazz, int requestCode) {
        Intent intent = getGoIntent(clazz);
        startActivityForResult(intent, requestCode);
    }

    /**
     * startActivityForResult with bundle
     *
     * @param clazz
     * @param requestCode
     * @param bundle
     */
    protected void readyGoForResult(Class<?> clazz, int requestCode, Bundle bundle) {
        Intent intent = getGoIntent(clazz);
        if (null != bundle) {
            intent.putExtras(bundle);
        }
        startActivityForResult(intent, requestCode);
    }

    /**
     * show toast
     *
     * @param msg
     */
    protected void showToast(String msg) {
        if (null != msg && !StringUtil.isEmpty(msg)) {
            Snackbar.make(((Activity) mContext).getWindow().getDecorView(), msg, Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * toggle show loading
     *
     * @param toggle
     */
    protected void toggleShowLoading(boolean toggle, String msg) {
        if (null == mVaryViewHelperController) {
            throw new IllegalArgumentException("You must return a right target view for loading");
        }

        if (toggle) {
            mVaryViewHelperController.showLoading(msg);
        } else {
            mVaryViewHelperController.restore();
        }
    }

    /**
     * toggle show empty
     *
     * @param toggle
     */
    protected void toggleShowEmpty(boolean toggle, String msg, View.OnClickListener onClickListener) {
        if (null == mVaryViewHelperController) {
            throw new IllegalArgumentException("You must return a right target view for loading");
        }

        if (toggle) {
            mVaryViewHelperController.showEmpty(msg, onClickListener);
        } else {
            mVaryViewHelperController.restore();
        }
    }

    /**
     * toggle show error
     *
     * @param toggle
     */
    protected void toggleShowError(boolean toggle, String msg, View.OnClickListener onClickListener) {
        if (null == mVaryViewHelperController) {
            throw new IllegalArgumentException("You must return a right target view for loading");
        }

        if (toggle) {
            mVaryViewHelperController.showError(msg, onClickListener);
        } else {
            mVaryViewHelperController.restore();
        }
    }

    /**
     * toggle show network error
     *
     * @param toggle
     */
    protected void toggleNetworkError(boolean toggle, View.OnClickListener onClickListener) {
        if (null == mVaryViewHelperController) {
            throw new IllegalArgumentException("You must return a right target view for loading");
        }

        if (toggle) {
            mVaryViewHelperController.showNetworkError(onClickListener);
        } else {
            mVaryViewHelperController.restore();
        }
    }

    public void onEventMainThread(EventCenter eventCenter) {
        if (null != eventCenter) {
            onEventComing(eventCenter);
        }
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void onEventBus(EventCenter eventCenter) {
        if (null != eventCenter) {
            onEventComing(eventCenter);
        }
    }

    /**
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     * Android6.0权限控制
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     * request permission
     *
     * @param dialogType  dialogType
     * @param requestCode requestCode
     * @param perms       permissions
     * @param callback    callback
     */

    public void performCodeWithPermission(@NonNull int dialogType,
                                          final int requestCode, @NonNull String[] perms, @NonNull RxQuickActivity.PermissionCallback callback) {
        if (getActivity() != null && getActivity() instanceof QuickActivity) {
            ((RxQuickActivity) getActivity()).performCodeWithPermission(dialogType, requestCode, perms, callback);
        }
    }

    /**
     * dialog
     */
    public void showLoadingDialog() {
        if (getActivity() != null && getActivity() instanceof QuickActivity) {
            ((QuickActivity) getActivity()).showLoadingDialog();
        }
    }

    public void showLoadingDialog(String tips) {
        if (getActivity() != null && getActivity() instanceof QuickActivity) {
            ((QuickActivity) getActivity()).showLoadingDialog(tips);
        }
    }

    public void dismissLoadingDialog() {
        if (getActivity() != null && getActivity() instanceof QuickActivity) {
            ((QuickActivity) getActivity()).dismissLoadingDialog();
        }
    }

    public CommonDialog.Builder getDialogBuilder(Context context) {
        if (getActivity() != null && getActivity() instanceof QuickActivity) {
            return ((QuickActivity) getActivity()).getDialogBuilder(context);
        }
        throw new IllegalStateException("CommonDialog can only be used in a class which extends QuickActivity!");
    }

}

