/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */package org.jkiss.dbeaver.ext.db2.model.plan;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

/**
 * DB2 execution plan analyser
 * 
 * @author Denis Forveille
 * 
 */
public class DB2PlanAnalyser implements DBCPlan {

   private static final Log        LOG         = LogFactory.getLog(DB2PlanAnalyser.class);

   // See init below
   private static String           PT_DELETE;
   private static final String     PT_EXPLAIN  = "EXPLAIN PLAN SET QUERYNO = %d FOR %s";
   private static final String     SEL_STMT    = "SELECT * FROM %s.EXPLAIN_STATEMENT WHERE QUERYNO = ? AND EXPLAIN_LEVEL = 'P' WITH UR";

   private static AtomicInteger    STMT_NO_GEN = new AtomicInteger(Long.valueOf(System.currentTimeMillis() / 10000000L).intValue());

   private String                  query;
   private String                  planTableSchema;

   private Collection<DB2PlanNode> listNodes;
   private DB2PlanStatement        db2PlanStatement;

   // ------------
   // Constructors
   // ------------

   public DB2PlanAnalyser(String query, String planTableSchema) {
      this.query = query;
      this.planTableSchema = planTableSchema;
   }

   // ----------------
   // Standard Getters
   // ----------------

   @Override
   public String getQueryString() {
      return query;
   }

   @Override
   public Collection<? extends DBCPlanNode> getPlanNodes() {
      return listNodes;
   }

   // ----------------
   // Business Methods
   // ----------------

   public void explain(JDBCExecutionContext context) throws DBCException {
      Integer stmtNo = STMT_NO_GEN.incrementAndGet();

      String explainStmt = String.format(PT_EXPLAIN, stmtNo, query);
      LOG.debug("Schema=" + planTableSchema + " : " + explainStmt);

      try {

         // Start by cleaning old rows for safety
         cleanExplainTables(context, stmtNo, planTableSchema);

         // Explain
         JDBCPreparedStatement dbStat = context.prepareStatement(String.format(PT_EXPLAIN, stmtNo, query));
         try {
            dbStat.execute();
         } finally {
            dbStat.close();
         }

         // Build Node Structure
         dbStat = context.prepareStatement(String.format(SEL_STMT, planTableSchema));
         try {
            dbStat.setInt(1, stmtNo);
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
               dbResult.next();
               db2PlanStatement = new DB2PlanStatement(context, dbResult, planTableSchema);
            } finally {
               dbResult.close();
            }
         } finally {
            dbStat.close();
         }

         listNodes = db2PlanStatement.buildNodes();

         // Clean afterward
         cleanExplainTables(context, stmtNo, planTableSchema);

      } catch (SQLException e) {
         throw new DBCException(e);
      }
   }

   // ----------------
   // Helpers
   // ----------------
   private void cleanExplainTables(JDBCExecutionContext context, Integer stmtNo, String planTableSchema) throws SQLException {
      // Delete previous statement rows
      JDBCPreparedStatement dbStat = context.prepareStatement(String.format(PT_DELETE, planTableSchema, planTableSchema));
      try {
         dbStat.setInt(1, stmtNo);
         dbStat.execute();
      } finally {
         dbStat.close();
      }
   }

   static {
      StringBuilder sb = new StringBuilder(256);
      sb.append("DELETE");
      sb.append("  FROM %s.EXPLAIN_INSTANCE I");
      sb.append(" WHERE EXISTS (SELECT 1");
      sb.append("                 FROM %s.EXPLAIN_STATEMENT S");
      sb.append("                WHERE S.EXPLAIN_TIME = I.EXPLAIN_TIME");
      sb.append("                  AND S.SOURCE_NAME = I.SOURCE_NAME");
      sb.append("                  AND S.SOURCE_SCHEMA = I.SOURCE_SCHEMA");
      sb.append("                  AND S.SOURCE_VERSION = I.SOURCE_VERSION");
      sb.append("                  AND QUERYNO = ?)");
      PT_DELETE = sb.toString();
   }

}