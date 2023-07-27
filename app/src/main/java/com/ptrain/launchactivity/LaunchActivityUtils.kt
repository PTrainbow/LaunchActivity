package com.ptrain.launchactivity

import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.Message
import android.os.MessageQueue
import android.util.Log

object LaunchActivityUtils {

    private const val TAG = "LaunchActivityUtils"
    private const val LAUNCH_ACTIVITY_ITEM_CLASS_NAME =
        "android.app.servertransaction.LaunchActivityItem"
    private const val ACTIVITY_RECORD_CLASS_NAME =
        "android.app.ActivityThread\$ActivityClientRecord"
    private const val CLIENT_TRANSACTION_CLASS_NAME =
        "android.app.servertransaction.ClientTransaction"

    private inline fun getIntentForTargetLaunch(next: (message: Message) -> Message): Intent? {
        var intent: Intent? = null
        try {
            // >= Android 9, msg.obj 为 ClientTransaction
            // < Android 9, msg.obj 为 ActivityClientRecord
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // step1: 获取 message queue
                val queue = Looper.getMainLooper().queue

                // step2: 获取 mMessages 属性，进而获取 msg.obj -> ClientTransaction
                var msg = ReflectUtils.reflect(queue).field("mMessages").get<Message>()
                msg = next(msg)
                val msgObj = msg.obj
                val className = msg.obj::class.java.name

                // 检测合法性
                if (className == CLIENT_TRANSACTION_CLASS_NAME) {
                    // step3: 获取 clientTransactionObj 的 mActivityCallbacks 属性
                    val activityCallbackList =
                        ReflectUtils.reflect(msgObj).field("mActivityCallbacks")
                            .get<List<*>>()

                    // 检测合法性
                    if (activityCallbackList.isNotEmpty()) {
                        // step4: 获取 intent
                        activityCallbackList[0]?.let {
                            if (it::class.java.name == LAUNCH_ACTIVITY_ITEM_CLASS_NAME) {
                                intent = ReflectUtils.reflect(it).field("mIntent").get<Intent>()
                            }
                        }
                    }
                }
            } else {
                // step1: Android 低版本 queue 方法不一致，反射获取 mQueue
                val looper = Looper.getMainLooper()
                val queue = ReflectUtils.reflect(looper).field("mQueue").get<MessageQueue>()

                // step2: 获取 mMessages 属性，进而获取 msg.obj -> ActivityClientRecord
                var msg = ReflectUtils.reflect(queue).field("mMessages").get<Message>()
                msg = next(msg)
                val msgObj = msg.obj
                val className = msgObj::class.java.name

                // step3: 获取 intent
                if (className == ACTIVITY_RECORD_CLASS_NAME) {
                    intent =
                        ReflectUtils.reflect(msgObj).field("intent").get<Intent>()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return intent
    }

    /**
     * application onCreate 时
     * 通过取 message 判断当前将要启动的是否为指定的 activity
     *
     * @param activityClassName activity 类名
     * @return true/false
     *
     */
    fun isTargetActivity(activityClassName: String): Boolean {
        val intent = getIntentForTargetLaunch {
            it
        }
        if (intent == null) {
            // next
            getIntentForTargetLaunch {
                ReflectUtils.reflect(it).field("next").get()
            }
        }
        Log.i(TAG, intent.toString())
        return intent?.component?.className?.contains(activityClassName)
            ?: false
    }
}