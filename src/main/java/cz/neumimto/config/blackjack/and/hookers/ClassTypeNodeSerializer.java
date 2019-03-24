package cz.neumimto.config.blackjack.and.hookers;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Modifier;

/**
 * Created by NeumimTo on 24.3.2019.
 */
public class ClassTypeNodeSerializer implements TypeSerializer<Object> {
    @Override
    public Object deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        Class<?> clazz = getInstantiableType(type, value.getNode("__class__").getString());
        return NotSoStupidObjectMapper.forClass(clazz).bindToNew().populate(value);
    }

    private Class<?> getInstantiableType(TypeToken<?> type, String configuredName) throws ObjectMappingException {
        Class<?> retClass;
        if (type.getRawType().isInterface() || Modifier.isAbstract(type.getRawType().getModifiers())) {
            if (configuredName == null) {
                throw new ObjectMappingException("No available configured type for instances of " + type);
            } else {
                try {
                    retClass = Class.forName(configuredName);
                } catch (ClassNotFoundException e) {
                    throw new ObjectMappingException("Unknown class of object " + configuredName, e);
                }
                if (!type.getRawType().isAssignableFrom(retClass)) {
                    throw new ObjectMappingException("Configured type " + configuredName + " does not extend "
                            + type.getRawType().getCanonicalName());
                }
            }
        } else {
            retClass = type.getRawType();
        }
        return retClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(@NonNull TypeToken<?> type, @Nullable Object obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (type.getRawType().isInterface() || Modifier.isAbstract(type.getRawType().getModifiers())) {
            // serialize obj's concrete type rather than the interface/abstract class
            value.getNode("__class__").setValue(obj.getClass().getName());
        }
        NotSoStupidObjectMapper.forObject(obj).serialize(value);
    }
}