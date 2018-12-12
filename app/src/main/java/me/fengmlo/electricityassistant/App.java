package me.fengmlo.electricityassistant;

import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import com.orhanobut.logger.*;
import me.fengmlo.AppExecutors;
import me.fengmlo.electricityassistant.database.AppDatabase;

public class App extends Application {

    private static App app;
    private static AppExecutors appExecutors;

    @Override
    public void onCreate() {
        super.onCreate();

        app = this;

        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(1)         // (Optional) How many method line to show. Default 2
                .methodOffset(5)        // (Optional) Hides internal method calls up to offset. Default 5
//                .logStrategy(customLog) // (Optional) Changes the log strategy to print out. Default LogCat
                .tag("LOGGER")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .logStrategy(new LogCatStrategy())
                .build();

        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return BuildConfig.DEBUG;
            }
        });

        Logger.i("onCreate");

        appExecutors = new AppExecutors();
    }

    public static class LogCatStrategy implements LogStrategy {

        private Handler handler;
        private long lastTime = SystemClock.uptimeMillis();
        private long offset = 5;

        public LogCatStrategy() {
            HandlerThread thread = new HandlerThread("thread_print");
            thread.start();
            handler = new Handler(thread.getLooper());
        }

        @Override
        public void log(final int priority, final String tag, @NonNull final String message) {

            lastTime += offset;
            if (lastTime < SystemClock.uptimeMillis()) {
                lastTime = SystemClock.uptimeMillis() + offset;
            }
            final long tmp = lastTime;
            handler.postAtTime(() -> Log.println(priority, tag, message), tmp);

        }
    }

    public static App getContext() {
        return app;
    }

    public static AppDatabase getDB() {
        return AppDatabase.getInstance(app);
    }

    public static void run(Runnable runnable) {
        appExecutors.networkIO().execute(runnable);
    }
}
