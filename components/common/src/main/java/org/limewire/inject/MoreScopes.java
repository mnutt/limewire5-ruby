package org.limewire.inject;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

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

}
