package com.androidwind.androidquick.demo.features.architecture.mvp.activity;

import com.androidwind.androidquick.demo.base.mvp.BaseContract;
import com.androidwind.androidquick.demo.base.mvp.BasePresenter;

/**
 * @author ddnosh
 * @website http://blog.csdn.net/ddnosh
 */
public interface MVPContract {
    interface Model extends BaseContract.BaseModel {

    }

    interface View extends BaseContract.BaseView {
        void refreshView(String result);
    }

    abstract class Presenter extends BasePresenter<View> {
        public abstract void initData();
    }
}
