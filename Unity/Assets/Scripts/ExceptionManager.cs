using UnityEngine;

public class ExceptionManager : LazySingleIstanceMonoBehaviour<ExceptionManager>
{
    void Awake()
    {
        Application.logMessageReceived += HandleException;
        DontDestroyOnLoad(this);
    }

    void HandleException(string logString, string stackTrace, LogType type)
    {
        if (type == LogType.Exception || type == LogType.Error)
        {
            if (NativeInterface.Instance != null)
            {
                NativeInterface.Instance.HandleException();
            }
        }
    }
}
