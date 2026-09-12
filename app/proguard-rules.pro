# Components declared in the manifest are kept by AGP automatically.
# WorkManager instantiates workers by class name, so keep the constructor.
-keep class com.balookrd.ibeacon.keepalive.WatchdogWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
