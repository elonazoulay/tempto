/*
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
package com.teradata.tempto.internal.fulfillment.table;

import com.teradata.tempto.fulfillment.table.TableDefinition;
import com.teradata.tempto.fulfillment.table.TableManager;
import com.teradata.tempto.query.QueryExecutionException;
import com.teradata.tempto.query.QueryExecutor;
import org.slf4j.Logger;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class AbstractTableManager<T extends TableDefinition>
        implements TableManager<T>
{
    private static final Logger LOGGER = getLogger(AbstractTableManager.class);

    private final QueryExecutor queryExecutor;
    private final TableNameGenerator tableNameGenerator;

    public AbstractTableManager(QueryExecutor queryExecutor, TableNameGenerator tableNameGenerator)
    {
        this.queryExecutor = requireNonNull(queryExecutor, "queryExecutor is null");
        this.tableNameGenerator = requireNonNull(tableNameGenerator, "tableNameGenerator is null");
    }

    @Override
    public void dropAllMutableTables()
    {
        try {
            DatabaseMetaData metaData = queryExecutor.getConnection().getMetaData();
            try (ResultSet tables = metaData.getTables(null, null, null, null)) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableNameGenerator.isMutableTableName(tableName)) {
                        dropTableIgnoreError(tableName);
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void dropTableIgnoreError(String tableNameInDatabase)
    {
        try {
            dropTable(tableNameInDatabase);
        }
        catch (QueryExecutionException ignored) {
            LOGGER.debug("{} - unable to drop table: {}", getDatabaseName(), tableNameInDatabase);
        }
    }

    @Override
    public void dropTable(String tableNameInDatabase)
    {
        queryExecutor.executeQuery("DROP TABLE " + tableNameInDatabase);
    }
}
