package org.limewire.inject;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

/**
 * Extensions to the default Guice Scoping.
 */
public class MoreScopes {

    /**
     * A singleton that will never be eager, in contrast to
     * {@link Scopes#SINGLETON}, which Guice eagerly creates sometimes.
     */
    public static final Scope LAZY_SINGLETON = new Scope() {
      public <T> Provider<T> scope(Key<T> key, Provider<T> creator) {
          return Scopes.SINGLETON.scope(key, creator);
      }

      @Override public String toString() {
        return "MoreScopes.LAZY_SINGLETON";
      }
    };
    
    /**
     * A singleton that will be eagerly loaded. A class with
     * an EagerSingleton annotation will be created at startup.
     */
    public static final Scope EAGER_SINGLETON = new Scope() {
        @Override
        public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
            return Scopes.SINGLETON.scope(key, unscoped);
        }        
        
        @Override public String toString() {
            return "MoreScopes.EAGER_SINGLETON";
        }
    };

}
