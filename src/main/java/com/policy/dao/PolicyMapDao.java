
package com.policy.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import com.policy.data.Customer;
import com.policy.data.Policy;

/*
 * This Class contains Select Queries for the following:
 * 		1. All Customer IDs based on Agent ID
 * 		2. All Policy IDs based on Agent ID and Customer ID
 * 		3. Information of Particular Policy based on Policy ID
 *   @author: Jian An Chiang 
 *   @version: 1.0
 *   @name: PolicyMapDao
 *   @Creation Date: August 15 2018 8:00PM 
 *   @History: Created Query Methods for 1 and 2  
 *   @Reviewed by:
 */
public class PolicyMapDao {
	private ArrayList<String> custid_list = new ArrayList<String>();	//Customer ID Array
	private ArrayList<String> policyid_list = new ArrayList<String>();  //Policy ID Array
	private String agentid;
	private String custid;
	private String policyid;
	
	
	public PolicyMapDao(){}
	
	
	public static boolean tagCustomer(Customer c, Policy p, String prem, String sumAssured,
			String[] medical, String agentID, String date, String[] nomineeNames, String[] nomineeRelations,
			String[] nomineePurpose) throws ClassNotFoundException, SQLException {
		Connection con = null;
		PreparedStatement ps = null;
		
		con = OracleConnection.INSTANCE.getConnection();
		String query = "select MAX(policy_map_id) from policymap";
		ps = con.prepareStatement(query);
		ResultSet rs = ps.executeQuery();
		rs.next();
		int id = rs.getInt(1);
		query = "insert into policymap values(?,?,?,?,?,?,?,?,?,?)";
		ps = con.prepareStatement(query);
		ps.setInt(1, id+1);
		ps.setInt(2, c.getCustomerId());
		ps.setInt(3, p.getPolicyId());
		ps.setInt(4, Integer.parseInt(agentID));
		ps.setDate(5, java.sql.Date.valueOf(date));
		ps.setInt(6, Integer.parseInt(prem));
		ps.setDouble(7, Double.parseDouble(sumAssured));
		ps.setString(8, Arrays.toString(medical));
		ps.setInt(9, 0);
		ps.setInt(10,-1);
		int rowsAffected = ps.executeUpdate();
		
		//clean up
		ps.close();
		rs.close();
		OracleConnection.INSTANCE.disconnect();
			
		if(rowsAffected >= 1) {
			
			System.out.println("Policy successfully added");
			//NomineeDao nomDao = new NomineeDao();
			int maxNomineeID = NomineeDao.maxNomineeID();
			int maxNomMapID = NomineeDao.maxNomineeMapID();
			for(int i = 0; i<nomineeNames.length; i++) {
				NomineeDao.insertNominee(nomineeNames[i], 
						nomineeRelations[i], nomineePurpose[i], maxNomineeID);
				NomineeDao.insertNomineeMap(maxNomMapID++,id+1, maxNomineeID++);
			}
			
			return true;
		}else {
			System.out.println("Policy was not added");
			return false;
		}
		
		
		
	}
	/*This constructor gets called in ViewPolicyByAgent and auto stores the ids whenever the jsp gets refreshed
	 * @param	
	 * 		agentid: The Agent ID inputed from ViewPolicyByAgent
	 * 		custid: The Customer ID inputed from ViewPolicyByAgent
	 * 		policyid: The Policy ID inputed from ViewPolicyByAgent
	 */
	public PolicyMapDao(String agentid, String custid, String policyid){
		this.agentid = agentid;
		this.custid = custid;
		this.policyid = policyid;
	}
	
	/*The getCustomers method takes in the agent id from ViewPolicyByAgent.jsp and queries all customer ids
	 * associated with the agent id.
	 * @param
	 * 		agentid: The Agent ID inputed from ViewPolicyByAgent
	 * @return
	 * 		custid_list: An ArrayList containing the customer ids are returned to ViewPolicyByAgent.jsp for
	 * 					 dynamic population
	 */
	public ArrayList<String> getCustomers(String agentid) throws SQLException, ClassNotFoundException{
		custid_list.clear();
		String SELECT_CUSTOMER_FROM_AGENT = "select customer_ID from PolicyMap where "
				+ "agent_ID = "+agentid;
		
		Connection con = null;
		PreparedStatement ps = null;
		
		con = OracleConnection.INSTANCE.getConnection();
	
		ps = con.prepareStatement(SELECT_CUSTOMER_FROM_AGENT);
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			int custid = rs.getInt("customer_ID");
			custid_list.add(Integer.toString(custid));
		}
		//clean up
		ps.close();
		OracleConnection.INSTANCE.disconnect();
		return custid_list;
	}
	
	/*The getPolicies method takes in the agent id and customer id from ViewPolicyByAgent.jsp and 
	 * queries all customer ids associated with the agent id and customer id.
	 * @param
	 * 		agentid: The Agent ID inputed from ViewPolicyByAgent
	 * 		custid: The Customer ID inputed from ViewPolicyByAgent
	 * @return
	 * 		custid_list: An ArrayList containing the policy ids are returned to ViewPolicyByAgent.jsp for
	 * 					 dynamic population
	 */
	public ArrayList<String> getPolicies(String agentid, String custid) throws SQLException, ClassNotFoundException{
		policyid_list.clear();
		
		final String SELECT_POLICIES_FROM_CUSTOMER = "select policy_ID from PolicyMap where "
				+ "customer_ID = "+custid+ " AND agent_ID = "+agentid;

		Connection con = null;
		PreparedStatement ps = null;
		
		con = OracleConnection.INSTANCE.getConnection();
	
		ps = con.prepareStatement(SELECT_POLICIES_FROM_CUSTOMER);
		ResultSet rs = ps.executeQuery();
		
		while (rs.next()) {
			int policyid = rs.getInt("policy_ID");
			policyid_list.add(Integer.toString(policyid));
		}
		//clean up
		ps.close();
		OracleConnection.INSTANCE.disconnect();
		return policyid_list;
	}
	
	/*The getPolicyInfo method takes in the agent id and customer id from ViewPolicyByAgent.jsp and 
	 * queries all customer ids associated with the agent id and customer id.
	 * @param
	 * 		agentid: The Agent ID inputed from ViewPolicyByAgent
	 * 		custid: The Customer ID inputed from ViewPolicyByAgent
	 * @return
	 * 		custid_list: An ArrayList containing the policy ids are returned to ViewPolicyByAgent.jsp for
	 * 					 dynamic population
	 */
	public Policy getPolicyInfo() throws ClassNotFoundException, SQLException { //QUERY POLICY DATA
		
		final String SELECT_INFO_FOR_POLICY = "select Policies.policy_id, policy_name, tenure, sum_assured_min, sum_assured_max, "
				+ "payments_per_year, premium_amount, start_date from Policies join PolicyMap on PolicyMap.policy_ID "
				+ "= Policies.policy_id where Policies.policy_id="+this.policyid;
	
		Connection con = null;
		PreparedStatement ps = null;
		
		con = OracleConnection.INSTANCE.getConnection();
		ps = con.prepareStatement(SELECT_INFO_FOR_POLICY);
		ResultSet rs = ps.executeQuery();
		Policy policy = new Policy();
		while (rs.next()) {
			int policyId = rs.getInt("policy_ID");
			String policyName = rs.getString("policy_name");
			double tenure = rs.getDouble("tenure");
			double sumAssuredMin = rs.getDouble("sum_assured_min");
			double sumAssuredMax = rs.getDouble("sum_assured_max");
			int paymentsPerYear = rs.getInt("payments_per_year");
			double premiumAmount = rs.getDouble("premium_amount");
			Date startDate = rs.getDate("start_date");
			policy.setPolicyId(policyId);
			policy.setPolicyName(policyName);
			policy.setTenure(tenure);
			policy.setMinSum(sumAssuredMin);
			policy.setMaxSum(sumAssuredMax);
			policy.setPaymentsPerYear(paymentsPerYear);
			policy.setPremiumAmount(premiumAmount);
			policy.setStartDate(startDate);
		}
		
		//clean up
		ps.close();
		OracleConnection.INSTANCE.disconnect();
		
		return policy;
	}
	
	public int getPolicyMapIDFromIDs(String custid, String policyid) throws SQLException, ClassNotFoundException{

		String SELECT_POLICY_MAP_ID_FROM_ID = "select policy_map_id from PolicyMap where "
				+ "policy_ID = "+policyid+" AND customer_ID = "+custid;

		Connection con = null;
		PreparedStatement ps = null;
		
		con = OracleConnection.INSTANCE.getConnection();
	
		ps = con.prepareStatement(SELECT_POLICY_MAP_ID_FROM_ID);
		ResultSet rs = ps.executeQuery();
		int policy_map_id=0;
		while (rs.next()) {
			policy_map_id = rs.getInt("policy_map_id");
		}
		//clean up
		ps.close();
		OracleConnection.INSTANCE.disconnect();
		return policy_map_id;
	}
	
	
	
	//Getters
	public String getAgentID(){
		return this.agentid;
	}
	public String getCustID() {
		return this.custid;
	}
	public String getPolicyID() {
		return this.policyid;
	}

}
