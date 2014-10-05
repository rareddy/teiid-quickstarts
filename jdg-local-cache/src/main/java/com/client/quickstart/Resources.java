/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.client.quickstart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.infinispan.commons.api.BasicCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.TransactionMode;

import com.client.quickstart.pojo.LineItem;
import com.client.quickstart.pojo.Orders;
import com.client.quickstart.pojo.Product;

/**
 * Note: the @Resource(lookup..) will look like its invalid in Eclipse, but will
 * build using maven. This is because "lookup" in Java 6 endorsed lib, and not
 * part of the classpath.
 * 
 * @author vhalbert
 * 
 */
public class Resources {
	private static Logger log = Logger.getLogger(Resources.class.getName());

	private static final String CACHE_NAME = "local-quickstart-cache";
	
	private static Resources resource = null;

	private static DefaultCacheManager container;
	private static boolean PRELOADED = false;

	@Produces
	@ApplicationScoped
	public static BasicCache<Object, Object> getCache() {
		if (!PRELOADED) {
			Resources r = new Resources();
			try {
				r.createContainer();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			r.loadCache();
			r.bindToJNDI();
			resource = r;
			PRELOADED = true;
		}
		return resource.getCacheFromContainer();
	}

	public static final int NUM_ORDERS = 10;
	public static final int NUM_PRODUCTS = 3;
	
	protected BasicCache<Object, Object> getCacheFromContainer() {
		return container.getCache(CACHE_NAME);
	}
	
	protected void bindToJNDI() {
		
		InitialContext ctx;
		try {
			ctx = new InitialContext();
			ctx.bind("jboss/teiid/jdg-quickstart", container);
			
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected  void loadCache() {
		BasicCache incache = container.getCache(CACHE_NAME, true);
		List<Product> products = new ArrayList<Product>(NUM_PRODUCTS);
		products.add(new Product(1, "Shirt", 54.99)); //$NON-NLS-1$
		products.add(new Product(2, "Pants", 89.00)); //$NON-NLS-1$
		products.add(new Product(3, "Socks", 1.29)); //$NON-NLS-1$

		int lineitems = 1;
		for (int i = 1; i <= NUM_ORDERS; i++) {

			List<LineItem> items = new ArrayList<LineItem>();
			for (int j = 0, p = 0, q = 1; j < lineitems; j++) {

				LineItem item = new LineItem(j + 1, products.get(p), q);

				items.add(item);

				++p;
				++q;
			}

			Orders order = new Orders(i, new Date(), "Person " + i, items); //$NON-NLS-1$

			incache.put(String.valueOf(i), order);

			++lineitems;
			if (lineitems > NUM_PRODUCTS) {
				lineitems = 1;
			}

		}
	} // end loadCache
	
	protected void createContainer() throws IOException {
		
//		 InputStream propStream = Resources.class.getClassLoader().getResourceAsStream("/com/client/quickstart/infinispan_persistent_indexing_config.xml");
//		 
//		 container = new DefaultCacheManager(propStream);
//	
        GlobalConfiguration glob = new GlobalConfigurationBuilder()
        .nonClusteredDefault() //Helper method that gets you a default constructed GlobalConfiguration, preconfigured for use in LOCAL mode
        .globalJmxStatistics().enable() //This method allows enables the jmx statistics of the global configuration.
       .jmxDomain("org.infinispan.orders")  //prevent collision with non-transactional carmart
        .build(); //Builds  the GlobalConfiguration object
    Configuration loc = new ConfigurationBuilder()
 //       .jmxStatistics().enable() //Enable JMX statistics
    	.indexing().enable().indexLocalOnly(true)
    	.addProperty("hibernate.search.lucene_version", "LUCENE_36")
    	.addProperty("lucene_version", "LUCENE_36")
    	
        .clustering().cacheMode(CacheMode.LOCAL) //Set Cache mode to LOCAL - Data is not replicated.
        .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
  //      .autoCommit(true) //Enable Transactional mode with autocommit false
  //      .lockingMode(LockingMode.OPTIMISTIC).transactionManagerLookup(new GenericTransactionManagerLookup()) //uses GenericTransactionManagerLookup - This is a lookup class that locate transaction managers in the most  popular Java EE application servers. If no transaction manager can be found, it defaults on the dummy transaction manager.
  //      .locking().isolationLevel(IsolationLevel.REPEATABLE_READ) //Sets the isolation level of locking
        .eviction().maxEntries(100).strategy(EvictionStrategy.LIRS) //Sets  4 as maximum number of entries in a cache instance and uses the LIRS strategy - an efficient low inter-reference recency set replacement policy to improve buffer cache performance
        .persistence().passivation(false).addSingleFileStore().purgeOnStartup(true) //Disable passivation and adds a SingleFileStore that is purged on Startup
        .build(); //Builds the Configuration object
    	container = new DefaultCacheManager(glob, loc, true);
    	container.startCache();
	}

} // class end
