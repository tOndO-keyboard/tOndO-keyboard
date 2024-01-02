using UnityEngine;

public class LazySingleIstanceMonoBehaviour<T> : MonoBehaviour where T : MonoBehaviour
{
    private static object _lock = new object();
    private static T instance;
    private static bool onDestroyDone = false;

    public static T Instance
    {
        get
        {
            if (onDestroyDone)
            {
                DebugLogger.Log("Someone tried to get a singleton instance after destroy was already done.", DebugLogger.LogType.WARNING);
                return null; 
            }
            lock (_lock)
            {
                if (instance == null)
                {
                    T[] instances = (T[])FindObjectsOfType(typeof(T));

                    if (instances.Length > 1)
                    {
                        DebugLogger.Log("More than one instance of '" + typeof(T) + "' was found!", DebugLogger.LogType.WARNING);
                    }

                    if (instances.Length <= 0)
                    {
                        instance = CreateInstance();
                        return instance;
                    }

                    instance = instances[0];

                    if (instance == null)
                    {
                        instance = CreateInstance();
                    }
                }

                return instance;
            }
        }
    }

    private static T CreateInstance()
    {
        GameObject gameObj = new GameObject();
        T component = gameObj.AddComponent<T>();
        gameObj.name = typeof(T).ToString();
        return component;
    }

    private void Awake()
    {
        onDestroyDone = false;
        if (instance != null && !((T)FindObjectOfType(typeof(T))).Equals(instance))
        {
            DebugLogger.Log("An instance of '" + typeof(T) + "' already exist.", DebugLogger.LogType.WARNING);
        }
    }

    private void OnDestroy()
    {
        onDestroyDone = true;
    }
}