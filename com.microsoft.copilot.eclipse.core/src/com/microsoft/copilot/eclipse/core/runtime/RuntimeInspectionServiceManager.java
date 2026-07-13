// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Central manager that discovers {@link IRuntimeInspectionService} providers registered as OSGi services and notifies
 * interested listeners when a provider becomes available.
 *
 * <p>This mirrors the decoupling used for terminal tools: Copilot never depends directly on a runtime provider (such as
 * CSIDE). Instead the provider registers an OSGi service and this manager tracks it, so the integration is optional and
 * degrades gracefully when no provider is installed.</p>
 */
public final class RuntimeInspectionServiceManager {

  /**
   * Listener for runtime inspection service availability changes.
   */
  public interface RuntimeServiceListener {
    /**
     * Called when a runtime inspection service becomes available.
     *
     * @param service the newly available service
     */
    void onServiceAvailable(IRuntimeInspectionService service);
  }

  /**
   * Lazy holder for the singleton instance.
   */
  private static final class InstanceHolder {
    private static final RuntimeInspectionServiceManager INSTANCE = createInstance();

    private static RuntimeInspectionServiceManager createInstance() {
      Bundle bundle = FrameworkUtil.getBundle(RuntimeInspectionServiceManager.class);
      if (bundle != null) {
        BundleContext context = bundle.getBundleContext();
        if (context != null) {
          RuntimeInspectionServiceManager instance = new RuntimeInspectionServiceManager(context);
          instance.start();
          return instance;
        }
      }
      return null;
    }
  }

  private final BundleContext bundleContext;
  private ServiceTracker<IRuntimeInspectionService, IRuntimeInspectionService> serviceTracker;
  private final CopyOnWriteArrayList<RuntimeServiceListener> listeners = new CopyOnWriteArrayList<>();

  private RuntimeInspectionServiceManager(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  /**
   * Returns the singleton instance, or {@code null} when the bundle context is not available (for example in some test
   * environments).
   *
   * @return the singleton instance or {@code null}
   */
  public static RuntimeInspectionServiceManager getInstance() {
    return InstanceHolder.INSTANCE;
  }

  private void start() {
    ServiceTrackerCustomizer<IRuntimeInspectionService, IRuntimeInspectionService> customizer =
        new ServiceTrackerCustomizer<>() {
          @Override
          public IRuntimeInspectionService addingService(ServiceReference<IRuntimeInspectionService> reference) {
            IRuntimeInspectionService service = bundleContext.getService(reference);
            if (service != null) {
              notifyServiceAvailable(service);
            }
            return service;
          }

          @Override
          public void modifiedService(ServiceReference<IRuntimeInspectionService> reference,
              IRuntimeInspectionService service) {
            // no-op
          }

          @Override
          public void removedService(ServiceReference<IRuntimeInspectionService> reference,
              IRuntimeInspectionService service) {
            bundleContext.ungetService(reference);
          }
        };

    serviceTracker = new ServiceTracker<>(bundleContext, IRuntimeInspectionService.class, customizer);
    serviceTracker.open();
  }

  /**
   * Adds a listener for service availability. If a service is already available the listener is notified immediately.
   *
   * @param listener the listener to add
   */
  public void addListener(RuntimeServiceListener listener) {
    if (listener == null) {
      return;
    }
    listeners.add(listener);
    for (IRuntimeInspectionService service : getServices()) {
      listener.onServiceAvailable(service);
    }
  }

  /**
   * Removes a previously added listener.
   *
   * @param listener the listener to remove
   */
  public void removeListener(RuntimeServiceListener listener) {
    listeners.remove(listener);
  }

  private void notifyServiceAvailable(IRuntimeInspectionService service) {
    for (RuntimeServiceListener listener : listeners) {
      listener.onServiceAvailable(service);
    }
  }

  /**
   * Returns all currently registered runtime inspection services.
   *
   * @return an unmodifiable list of services, never {@code null}
   */
  public List<IRuntimeInspectionService> getServices() {
    List<IRuntimeInspectionService> result = new ArrayList<>();
    if (serviceTracker != null) {
      IRuntimeInspectionService[] services = serviceTracker.getServices(new IRuntimeInspectionService[0]);
      if (services != null) {
        for (IRuntimeInspectionService service : services) {
          if (service != null) {
            result.add(service);
          }
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Indicates whether at least one runtime inspection service is registered.
   *
   * @return {@code true} if a provider is available
   */
  public boolean isAvailable() {
    return !getServices().isEmpty();
  }

  /**
   * Stops tracking services and clears listeners.
   */
  public void stop() {
    if (serviceTracker != null) {
      serviceTracker.close();
      serviceTracker = null;
    }
    listeners.clear();
  }
}
