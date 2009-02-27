/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http.transformer;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

/**
 * Message Transformer that expects a {@link HttpServletRequest} as input and binds
 * its parameter map to a target instance. The target instance may be a non-singleton
 * bean as specified by the {@link #prototypeBeanName} property. Otherwise, this
 * transformer's {@link #type} must provide a default no-arg constructor.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public class ServletRequestBindingTransformer<T> extends AbstractPayloadTransformer<HttpServletRequest, T> implements BeanFactoryAware {

	private final Class<T> targetType;

	private volatile String prototypeBeanName;

	private volatile WebBindingInitializer webBindingInitializer;

	private volatile BeanFactory beanFactory;


	public ServletRequestBindingTransformer(Class<T> targetType) {
		Assert.notNull(targetType, "targetType must not be null");
		this.targetType = targetType;
	}


	public void setPrototypeBeanName(String prototypeBeanName) {
		this.prototypeBeanName = prototypeBeanName;
	}

	public void setWebBindingInitializer(WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T transformPayload(HttpServletRequest request) throws Exception {
		ServletRequestDataBinder binder = new ServletRequestDataBinder(getTarget());
		this.initBinder(binder, request);
		binder.bind(request);
		// this will immediately throw any bind Exceptions
		Map map = binder.close();
		return (T) map.get(ServletRequestDataBinder.DEFAULT_OBJECT_NAME);
	}

	private void initBinder(ServletRequestDataBinder binder, HttpServletRequest request) {
		if (this.webBindingInitializer != null) {
			this.webBindingInitializer.initBinder(binder, new DispatcherServletWebRequest(request));
		}
	}

	@SuppressWarnings("unchecked")
	private Object getTarget() throws InstantiationException, IllegalAccessException {
		if (this.prototypeBeanName == null) {
			return this.targetType.newInstance();
		}
		Assert.notNull(this.beanFactory, "beanFactory is required for binding to a prototype bean");
		if (this.beanFactory.isSingleton(this.prototypeBeanName)) {
			throw new IllegalArgumentException("binding target bean must not be a singleton");
		}
		return (T) this.beanFactory.getBean(this.prototypeBeanName, this.targetType);
	}

}
