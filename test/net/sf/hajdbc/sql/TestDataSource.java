/**
 * HA-JDBC: High-Availability JDBC
 * Copyright (c) 2004-2006 Paul Ferraro
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by the 
 * Free Software Foundation; either version 2.1 of the License, or (at your 
 * option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, 
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Contact: ferraro@users.sourceforge.net
 */
package net.sf.hajdbc.sql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.sf.hajdbc.Balancer;
import net.sf.hajdbc.Database;
import net.sf.hajdbc.DatabaseCluster;
import net.sf.hajdbc.MockDatabase;
import net.sf.hajdbc.util.reflect.ProxyFactory;

import org.easymock.EasyMock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@SuppressWarnings("unchecked")
public class TestDataSource implements javax.sql.DataSource
{
	private DatabaseCluster cluster = EasyMock.createStrictMock(DatabaseCluster.class);
	private Balancer balancer = EasyMock.createStrictMock(Balancer.class);
	private javax.sql.DataSource dataSource1 = EasyMock.createStrictMock(javax.sql.DataSource.class);
	private javax.sql.DataSource dataSource2 = EasyMock.createStrictMock(javax.sql.DataSource.class);

	private Database database1 = new MockDatabase("1");
	private Database database2 = new MockDatabase("2");
	private Set<Database> databaseSet;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private javax.sql.DataSource dataSource;

	@BeforeClass
	void init()
	{
		Map<Database, javax.sql.DataSource> map = new TreeMap<Database, javax.sql.DataSource>();
		map.put(this.database1, this.dataSource1);
		map.put(this.database2, this.dataSource2);
		
		this.databaseSet = map.keySet();
		
		EasyMock.expect(this.cluster.getConnectionFactoryMap()).andReturn(map);
		
		this.replay();
		
		this.dataSource = ProxyFactory.createProxy(javax.sql.DataSource.class, new DataSourceInvocationHandler(this.cluster));

		this.verify();
		this.reset();
	}
	
	void replay()
	{
		EasyMock.replay(this.cluster, this.balancer, this.dataSource1, this.dataSource2);
	}
	
	void verify()
	{
		EasyMock.verify(this.cluster, this.balancer, this.dataSource1, this.dataSource2);
	}
	
	@AfterMethod
	void reset()
	{
		EasyMock.reset(this.cluster, this.balancer, this.dataSource1, this.dataSource2);
	}
	
	/**
	 * @see javax.sql.DataSource#getConnection()
	 */
	@Test
	public Connection getConnection() throws SQLException
	{
		Connection connection1 = EasyMock.createStrictMock(Connection.class);
		Connection connection2 = EasyMock.createStrictMock(Connection.class);
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.dataSource1.getConnection()).andReturn(connection1);
		EasyMock.expect(this.dataSource2.getConnection()).andReturn(connection2);
		
		this.replay();
		
		Connection result = this.dataSource.getConnection();
		
		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == connection1;
		assert proxy.getObject(this.database2) == connection2;
		
		return result;
	}

	@DataProvider(name = "connect")
	Object[][] connectProvider()
	{
		return new Object[][] { new Object[] { "", "" } };
	}

	/**
	 * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
	 */
	@Test(dataProvider = "connect")
	public Connection getConnection(String user, String password) throws SQLException
	{
		Connection connection1 = EasyMock.createStrictMock(Connection.class);
		Connection connection2 = EasyMock.createStrictMock(Connection.class);
		
		
		EasyMock.expect(this.cluster.getBalancer()).andReturn(this.balancer);
		EasyMock.expect(this.balancer.all()).andReturn(this.databaseSet);
		
		EasyMock.expect(this.cluster.getNonTransactionalExecutor()).andReturn(this.executor);
		
		EasyMock.expect(this.dataSource1.getConnection(user, password)).andReturn(connection1);
		EasyMock.expect(this.dataSource2.getConnection(user, password)).andReturn(connection2);
		
		this.replay();
		
		Connection result = this.dataSource.getConnection(user, password);
		
		this.verify();
		
		assert Proxy.isProxyClass(result.getClass());
		
		SQLProxy proxy = SQLProxy.class.cast(Proxy.getInvocationHandler(result));
		
		assert proxy.getObject(this.database1) == connection1;
		assert proxy.getObject(this.database2) == connection2;
		
		return result;
	}

	/**
	 * @see javax.sql.CommonDataSource#getLogWriter()
	 */
	@Test
	public PrintWriter getLogWriter() throws SQLException
	{
		PrintWriter writer = new PrintWriter(System.out);
		
		EasyMock.expect(this.dataSource1.getLogWriter()).andReturn(writer);
		
		this.replay();
		
		PrintWriter result = this.dataSource.getLogWriter();
		
		this.verify();
		
		assert result == writer;
		
		return result;
	}

	/**
	 * @see javax.sql.CommonDataSource#getLoginTimeout()
	 */
	@Test
	public int getLoginTimeout() throws SQLException
	{
		int timeout = 1;
		
		EasyMock.expect(this.dataSource1.getLoginTimeout()).andReturn(timeout);
		
		this.replay();
		
		int result = this.dataSource.getLoginTimeout();

		this.verify();
		
		assert result == timeout;
		
		return result;
	}

	@DataProvider(name = "writer")
	Object[][] writerProvider()
	{
		return new Object[][] { new Object[] { new PrintWriter(new StringWriter()) } };
	}
	
	/**
	 * @see javax.sql.CommonDataSource#setLogWriter(java.io.PrintWriter)
	 */
	@Test(dataProvider = "writer")
	public void setLogWriter(PrintWriter writer) throws SQLException
	{
		this.dataSource1.setLogWriter(writer);
		this.dataSource2.setLogWriter(writer);

		this.replay();
		
		this.dataSource.setLogWriter(writer);
		
		this.verify();
	}

	@DataProvider(name = "int")
	Object[][] timeoutProvider()
	{
		return new Object[][] { new Object[] { 0 } };
	}

	/**
	 * @see javax.sql.CommonDataSource#setLoginTimeout(int)
	 */
	@Test(dataProvider = "int")
	public void setLoginTimeout(int timeout) throws SQLException
	{
		this.dataSource1.setLoginTimeout(timeout);
		this.dataSource2.setLoginTimeout(timeout);

		this.replay();
		
		this.dataSource.setLoginTimeout(timeout);
		
		this.verify();
	}

	@DataProvider(name = "class")
	Object[][] classProvider()
	{
		return new Object[][] { new Object[] { Object.class } };
	}

	/**
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Test(dataProvider = "class")
	public boolean isWrapperFor(Class<?> targetClass) throws SQLException
	{
		EasyMock.expect(this.dataSource1.isWrapperFor(targetClass)).andReturn(true);
		
		this.replay();
		
		boolean result = this.dataSource.isWrapperFor(targetClass);

		this.verify();
		
		assert result;
		
		return result;
	}

	/**
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Test(dataProvider = "class")
	public <T> T unwrap(Class<T> targetClass) throws SQLException
	{
		try
		{
			T object = targetClass.newInstance();
			
			EasyMock.expect(this.dataSource1.unwrap(targetClass)).andReturn(object);
			
			this.replay();

			T result = this.dataSource.unwrap(targetClass);

			this.verify();
			
			assert result == object;
			
			return result;
		}
		catch (InstantiationException e)
		{
			assert false : e;
		}
		catch (IllegalAccessException e)
		{
			assert false : e;
		}
		
		return null;
	}
}
