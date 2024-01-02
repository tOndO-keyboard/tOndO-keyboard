using System.Diagnostics;

public static class DebugLogger
{
    public enum LogType
    { ERROR, WARNING, INFO }

    private static readonly string logPrefix = "[TondoKeyboard]";

    public static void Log(string message, LogType type = LogType.INFO)
    {
        System.Diagnostics.StackTrace stackTrace = new System.Diagnostics.StackTrace(true);
        StackFrame sf = stackTrace.GetFrame(1);
        string m = string.Format("{0} {1} at {2} {3} line {4}", logPrefix, message, sf.GetMethod().DeclaringType, sf.GetMethod().Name, sf.GetFileLineNumber());
        switch (type)
        {
#if DEBUG_MODE
            case LogType.INFO:
                UnityEngine.Debug.Log(m);
                break;
#endif
            case LogType.WARNING:
                UnityEngine.Debug.LogWarning(m);
                break;
            case LogType.ERROR:
                UnityEngine.Debug.LogError(m);
                break;
        }
    }
}
