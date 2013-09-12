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
 */
package org.jkiss.dbeaver.ext.db2.model.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.ext.db2.model.DB2TableKeyColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;

/**
 * Cache for DB2 Table Foreign Keys
 * 
 * @author Denis Forveille
 * 
 */
public final class DB2TableForeignKeyCache extends JDBCCompositeCache<DB2Schema, DB2Table, DB2TableForeignKey, DB2TableKeyColumn> {

   private static String SQL_FK_TAB;
   private static String SQL_FK_ALL;

   static {
      StringBuilder sb = new StringBuilder(512);
      sb.append(" SELECT R.*");
      sb.append("      , KCU.COLNAME");
      sb.append("      , KCU.COLSEQ");
      sb.append("   FROM SYSCAT.REFERENCES R");
      sb.append("       ,SYSCAT.KEYCOLUSE KCU");
      sb.append("  WHERE R.TABSCHEMA = ?");
      sb.append("    AND R.TABNAME = ?");
      sb.append("    AND KCU.CONSTNAME = R.CONSTNAME");
      sb.append("    AND KCU.TABSCHEMA = R.TABSCHEMA");
      sb.append("    AND KCU.TABNAME   = R.TABNAME");
      sb.append("  ORDER BY R.CONSTNAME");
      sb.append("         , KCU.COLSEQ");
      sb.append(" WITH UR");
      SQL_FK_TAB = sb.toString();

      sb.setLength(0);

      sb.append(" SELECT R.*");
      sb.append("      , KCU.COLNAME");
      sb.append("      , KCU.COLSEQ");
      sb.append("   FROM SYSCAT.REFERENCES R");
      sb.append("       ,SYSCAT.KEYCOLUSE KCU");
      sb.append("  WHERE R.TABSCHEMA = ?");
      sb.append("    AND KCU.CONSTNAME = R.CONSTNAME");
      sb.append("    AND KCU.TABSCHEMA = R.TABSCHEMA");
      sb.append("    AND KCU.TABNAME   = R.TABNAME");
      sb.append("  ORDER BY R.CONSTNAME");
      sb.append("         , KCU.COLSEQ");
      sb.append(" WITH UR");
      SQL_FK_ALL = sb.toString();
   }

   public DB2TableForeignKeyCache(DB2TableCache tableCache) {
      super(tableCache, DB2Table.class, "TABNAME", "CONSTNAME");
   }

   @Override
   protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Schema db2Schema, DB2Table forTable) throws SQLException {

      String sql;
      if (forTable != null) {
         sql = SQL_FK_TAB;
      } else {
         sql = SQL_FK_ALL;
      }
      JDBCPreparedStatement dbStat = context.prepareStatement(sql);
      dbStat.setString(1, db2Schema.getName());
      if (forTable != null) {
         dbStat.setString(2, forTable.getName());
      }
      return dbStat;
   }

   @Override
   protected DB2TableForeignKey fetchObject(JDBCExecutionContext context,
                                            DB2Schema db2Schema,
                                            DB2Table db2Table,
                                            String constName,
                                            ResultSet dbResult) throws SQLException, DBException {
      return new DB2TableForeignKey(context.getProgressMonitor(), db2Table, dbResult);
   }

   @Override
   protected DB2TableKeyColumn fetchObjectRow(JDBCExecutionContext context,
                                              DB2Table db2Table,
                                              DB2TableForeignKey object,
                                              ResultSet dbResult) throws SQLException, DBException {

      String colName = JDBCUtils.safeGetString(dbResult, "COLNAME");
      DB2TableColumn tableColumn = DB2Table.findTableColumn(context.getProgressMonitor(), db2Table, colName);
      if (tableColumn == null) {
         log.debug("DB2TableForeignKeyCache : Column '" + colName + "' not found in table '" + db2Table.getFullQualifiedName()
                  + "' ??");
         return null;
      } else {
         return new DB2TableKeyColumn(object, tableColumn, JDBCUtils.safeGetInt(dbResult, "COLSEQ"));
      }
   }

   @Override
   protected void cacheChildren(DB2TableForeignKey constraint, List<DB2TableKeyColumn> rows) {
      constraint.setColumns(rows);
   }
}