package org.jboss.resteasy.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import org.jboss.resteasy.spi.metadata.Parameter;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.resteasy.spi.metadata.ResourceLocator;
import org.jboss.resteasy.spi.util.PickConstructor;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface InjectorFactory {

    /**
     * Attempts to create a constructor injector for a class without pre-selecting a constructor. This allows injector
     * factories (such as CDI) to handle classes that may not have public constructors.
     * <p>
     * The default implementation uses {@link PickConstructor#pickSingletonConstructor(Class)} to selecta constructor
     * and delegates to {@link #createConstructor(Constructor, ResteasyProviderFactory)}. Implementations can override
     * this to provide custom constructor selection logic.
     * </p>
     *
     * @param clazz           the class to create a constructor injector for
     * @param providerFactory the provider factory
     * @return a constructor injector, or {@code null} if no suitable constructor can be found
     */
    default ConstructorInjector createConstructor(final Class<?> clazz, final ResteasyProviderFactory providerFactory) {
        final Constructor<?> constructor = PickConstructor.pickSingletonConstructor(clazz);
        if (constructor == null) {
            return null;
        }
        return createConstructor(constructor, providerFactory);
    }

    ConstructorInjector createConstructor(Constructor constructor, ResteasyProviderFactory factory);

    PropertyInjector createPropertyInjector(Class resourceClass, ResteasyProviderFactory factory);

    ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName,
            Class type, Type genericType, Annotation[] annotations, ResteasyProviderFactory factory);

    ValueInjector createParameterExtractor(Class injectTargetClass, AccessibleObject injectTarget, String defaultName,
            Class type, Type genericType, Annotation[] annotations, boolean useDefault, ResteasyProviderFactory factory);

    ValueInjector createParameterExtractor(Parameter parameter, ResteasyProviderFactory providerFactory);

    MethodInjector createMethodInjector(ResourceLocator method, ResteasyProviderFactory factory);

    PropertyInjector createPropertyInjector(ResourceClass resourceClass, ResteasyProviderFactory providerFactory);

    ConstructorInjector createConstructor(ResourceConstructor constructor, ResteasyProviderFactory providerFactory);
}
