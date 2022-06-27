package com.utils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

public class IgniteUtils {

	public static Ignite createServerInstance(List<String> ipList, String instanceName, int initialLocalPort, int endPort, int localPort) {
		List<String> ipRangeList = ipList.stream().map(ip -> ip + ":" + initialLocalPort + ".." + endPort).collect(Collectors.toList());
		IgniteConfiguration firstCfg = new IgniteConfiguration();
		firstCfg.setIgniteInstanceName(instanceName);
		// Explicitly configure TCP discovery SPI to provide list of initial nodes
		// from the first cluster.
		TcpDiscoverySpi firstDiscoverySpi = new TcpDiscoverySpi();
		// Initial local port to listen to.
		firstDiscoverySpi.setLocalPort(initialLocalPort);
		// Changing local port range. This is an optional action.
		firstDiscoverySpi.setLocalPortRange(endPort - initialLocalPort);
		TcpDiscoveryVmIpFinder firstIpFinder = new TcpDiscoveryVmIpFinder();
		// Addresses and port range of the nodes from the first cluster.
		// 127.0.0.1 can be replaced with actual IP addresses or host names.
		// The port range is optional.
		firstIpFinder.setAddresses(ipRangeList);
		// Overriding IP finder.
		firstDiscoverySpi.setIpFinder(firstIpFinder);
		// Explicitly configure TCP communication SPI by changing local port number for
		// the nodes from the first cluster.
		TcpCommunicationSpi firstCommSpi = new TcpCommunicationSpi();
		firstCommSpi.setLocalPort(localPort);
		// Overriding discovery SPI.
		firstCfg.setDiscoverySpi(firstDiscoverySpi);
		// Overriding communication SPI.
		firstCfg.setCommunicationSpi(firstCommSpi);
		// Starting a node.
		Ignite ignite = Ignition.start(firstCfg);
		return ignite;
	}
}
