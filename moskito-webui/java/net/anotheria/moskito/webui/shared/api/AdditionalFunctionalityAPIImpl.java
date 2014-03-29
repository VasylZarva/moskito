package net.anotheria.moskito.webui.shared.api;

import net.anotheria.anoplass.api.APIException;
import net.anotheria.moskito.core.config.plugins.PluginConfig;
import net.anotheria.moskito.core.plugins.PluginRepository;
import net.anotheria.moskito.core.stats.Interval;
import net.anotheria.moskito.core.stats.impl.IntervalRegistry;
import net.anotheria.moskito.core.timing.IUpdateable;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * TODO comment this class
 *
 * @author lrosenberg
 * @since 24.03.14 22:53
 */
public class AdditionalFunctionalityAPIImpl extends AbstractMoskitoAPIImpl implements AdditionalFunctionalityAPI{
	@Override
	public List<PluginAO> getPlugins() throws APIException {
		List<String> pluginNames = PluginRepository.getInstance().getPluginNames();
		ArrayList<PluginAO> ret = new ArrayList<PluginAO>();
		for (String s : pluginNames){
			PluginAO ao = new PluginAO();

			ao.setName(s);
			try{
				ao.setDescription(""+PluginRepository.getInstance().getPlugin(s));
			}catch(Exception e){
				ao.setDescription("Error: "+e.getMessage());
			}

			PluginConfig config = PluginRepository.getInstance().getConfig(s);
			if (config==null){
				ao.setClassName("-not found-");
				ao.setConfigurationName("-not found-");
			}else{
				ao.setConfigurationName(config.getConfigurationName());
				ao.setClassName(config.getClassName());
			}
			ret.add(ao);
		}
		return ret;

	}

	@Override
	public void removePlugin(String pluginName) throws APIException {
		PluginRepository.getInstance().removePlugin(pluginName);
	}

	@Override
	public void forceIntervalUpdate(String intervalName) throws APIException {
		IntervalRegistry registry = IntervalRegistry.getInstance();
		Interval interval = registry.getInterval(intervalName);
		((IUpdateable)interval).update();
	}

	@Override public List<MBeanWrapperAO> getMBeans() throws APIException{
		try{
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
			List<MBeanWrapperAO> beans = new ArrayList<MBeanWrapperAO>();

			for (MBeanServer s : servers){
				Set<ObjectInstance> instances = s.queryMBeans(null, null);
				for (ObjectInstance oi : instances){
					MBeanWrapperAO bean = new MBeanWrapperAO();
					bean.setClassName(oi.getClassName());
					ObjectName name = oi.getObjectName();
					bean.setDomain(name.getDomain());
					bean.setCanonicalName(name.getCanonicalName());
					String type = name.getKeyProperty("type");
					if (type!=null){
						bean.setType(type);
					}

					MBeanInfo info = s.getMBeanInfo(name);
					if (info!=null){
						bean.setDescription(info.getDescription());
						bean.setAttributes(convert(info.getAttributes(), s, name));
						bean.setOperations(Arrays.asList(info.getOperations()));
					}
					beans.add(bean);
				}
			}
			return beans;
		}catch(JMException e){
			throw new APIException("JMX Failure "+e.getMessage(), e);
		}
	}

	/**
	 * @param infos
	 *            the {@link javax.management.MBeanAttributeInfo} to wrap
	 * @param server
	 *            {@link MBeanServer} where to find the MBean values
	 * @param name
	 *            {@link ObjectName} where to find the MBean values
	 * @return the converted list of {@link MBeanAttributeWrapperAO}s
	 */
	private List<MBeanAttributeWrapperAO> convert(final MBeanAttributeInfo[] infos,
												final MBeanServer server, final ObjectName name) {
		final List<MBeanAttributeWrapperAO> res = new ArrayList<MBeanAttributeWrapperAO>();

		for (final MBeanAttributeInfo info : infos) {
			Object value = "-";
			try {
				value = server.getAttribute(name, info.getName());
				if (value instanceof Object[]){
					value = Arrays.asList((Object[])value);
				}

				// CHECKSTYLE:OFF - we have to catch ALL exceptions
			} catch (final Exception e) {
				// CHECKSTYLE:ON
				log.debug("unable to read MBean: " + e.getLocalizedMessage());
			}

			res.add(new MBeanAttributeWrapperAO(info, value));
		}

		return res;
	}

}