package botsimp.testbot24;

import java.util.HashMap;

public class DefaultDict<K, V> extends HashMap<K, V> {
    Class<V> klass;

    @SuppressWarnings("unchecked")
    public DefaultDict(Class<?> klass) {
        this.klass = (Class<V>) klass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V returnValue = super.get(key);
        if (returnValue == null) {
            try {
                returnValue = klass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            this.put((K) key, returnValue);
        }
        return returnValue;
    }
}
