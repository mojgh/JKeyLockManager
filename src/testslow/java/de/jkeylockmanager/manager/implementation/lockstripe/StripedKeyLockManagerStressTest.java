/*
 * Copyright 2009 Marc-Olaf Jaschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.jkeylockmanager.manager.implementation.lockstripe;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;

/**
 * Stress test for {@link StripedKeyLockManager}.
 *
 * @author Marc-Olaf Jaschke
 *
 */
public class StripedKeyLockManagerStressTest {

	private static final int LOCK_TIMEOUT = 1;
	private static final int DIFFERENT_KEYS = 5;
	private static final int THREAD_COUNT = 200;
	private static final int INVOCATIONS_PER_THREAD = 1000;


	@Test
	public void testDoLocked() throws InterruptedException {

		final StripedKeyLockManager manager = new StripedKeyLockManager(LOCK_TIMEOUT, TimeUnit.HOURS, KeyLockManagers.DEFAULT_NUMBER_OF_STRIPES);
		final WeatherService service = new WeatherService();
		final WeatherServiceProxy serviceProxy = new WeatherServiceProxy(service, manager);

		final List<String> keys = new ArrayList<>();
		for (int i = 0; i < DIFFERENT_KEYS; i++) {
			keys.add(Integer.toString(i));
		}

		final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

		final CyclicBarrier waitForOtherThreads = new CyclicBarrier(THREAD_COUNT);

		for (int i = 0; i < THREAD_COUNT; i++) {
			executorService.execute(new TestThread(serviceProxy, keys, waitForOtherThreads));
		}

		executorService.shutdown();
		if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
			fail("executor service failed to shutdown");
		}

		assertFalse(service.concurrentUpdatesPerKey());

		assertEquals("all locks must me disposed", 0, manager.activeKeyLocksCount());

	}


    private interface IWeatherService {
        void updateWeatherData(String cityName);
    }

    private static class TestThread implements Runnable {

        private final IWeatherService service;
        private final List<String> keys;
        private final Random random = new Random();
        private final CyclicBarrier waitForOtherThreads;

        public TestThread(final IWeatherService service, final List<String> keys, final CyclicBarrier waitForOtherThreads) {
            this.service = service;
            this.keys = new ArrayList<>(keys);
            this.waitForOtherThreads = waitForOtherThreads;
        }

        @Override
		public void run() {
            try {
                waitForOtherThreads.await();
                for (int i = 0; i < INVOCATIONS_PER_THREAD; i++) {
                    final String key = keys.get(random.nextInt(keys.size()));
                    service.updateWeatherData(key);
                }
            } catch (final InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    private static class WeatherService implements IWeatherService {

        private final Map<String, Integer> invocationsSafe = new HashMap<>();
        private final Map<String, Integer> invocationsUnsafe = new ConcurrentHashMap<>();

        public boolean concurrentUpdatesPerKey() {
            boolean result = false;
            for (final Entry<String, Integer> entry : invocationsSafe.entrySet()) {
                if (!entry.getValue().equals(invocationsUnsafe.get(entry.getKey()))) {
                    result = true;
                    break;
                }
            }
            return result;
        }

        @Override
		public void updateWeatherData(final String cityName) {
            synchronized (this) {
                incCounter(cityName, invocationsSafe, false);
            }
            incCounter(cityName, invocationsUnsafe, true);
        }

        private void incCounter(final String cityName, final Map<String, Integer> invocations, final boolean wait) {
            final Integer count = invocations.get(cityName);
            if (wait) {
                try {
                    sleep(0, 10);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            invocations.put(cityName, count == null ? 1 : count + 1);
        }
    }

    private static class WeatherServiceProxy implements IWeatherService {

        private final IWeatherService delegate;
        private final KeyLockManager lock;

        public WeatherServiceProxy(final IWeatherService delegate, final KeyLockManager lock) {
            this.delegate = delegate;
            this.lock = lock;
        }

        @Override
		public void updateWeatherData(final String cityName) {
            lock.executeLocked(cityName, () -> delegate.updateWeatherData(cityName));
        }

    }
}
