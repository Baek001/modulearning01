param(
    [string]$MapperDir = ".\src\main\resources\kr\or\ddit\mybatis\mapper",
    [string]$OutputDir = ".\migration-input\ddl"
)

$ErrorActionPreference = "Stop"

function Get-SingularName {
    param([string]$TableName)
    if ($TableName.EndsWith("IES")) {
        return $TableName.Substring(0, $TableName.Length - 3) + "Y"
    }
    if ($TableName.EndsWith("S") -and -not $TableName.EndsWith("SS")) {
        return $TableName.Substring(0, $TableName.Length - 1)
    }
    return $TableName
}

function Get-ColumnType {
    param([string]$ColumnName)

    if ($ColumnName -match "(CONTENT|CONTENTS|BODY|MEMO|REASON|PATH|HTML|TEXT|COMMENT|DESC)$") { return "TEXT" }
    if ($ColumnName -match "(^CRT_DT$|^UPD_DT$|^REG_DT$|^MOD_DT$|^SEND_DT$|^READ_DT$|^START_DT$|^END_DT$|^HIRE_YMD$|^RSGNTN_YMD$)") { return "TIMESTAMP" }
    if ($ColumnName -match "(_DT|_DATE|_YMD|_TIME)$") { return "TIMESTAMP" }
    if ($ColumnName -match "(^SORT_NUM$|_CNT$|_COUNT$|_NUM$|_SEQ$|_NO$|^LEVEL$|^RN$)") { return "INTEGER" }
    if ($ColumnName -match "(_AMT$|_RATE$|_PCT$)") { return "NUMERIC(18,2)" }
    if ($ColumnName -match "_YN$") { return "CHAR(1)" }
    if ($ColumnName -match "(PSWD|PASSWORD)$") { return "VARCHAR(255)" }
    if ($ColumnName -match "EMAIL$") { return "VARCHAR(255)" }
    if ($ColumnName -match "(TEL|TELNO|PHONE)$") { return "VARCHAR(30)" }
    if ($ColumnName -match "URL$") { return "VARCHAR(500)" }
    if ($ColumnName -match "_NM$|_NAME$") { return "VARCHAR(200)" }
    if ($ColumnName -match "_CD$") { return "VARCHAR(40)" }
    if ($ColumnName -match "_ID$") { return "VARCHAR(80)" }
    return "VARCHAR(255)"
}

function Add-Table {
    param(
        [hashtable]$Tables,
        [string]$TableName
    )
    if (-not $Tables.ContainsKey($TableName)) {
        $Tables[$TableName] = [ordered]@{
            Columns = New-Object "System.Collections.Specialized.OrderedDictionary"
        }
    }
}

function Add-Column {
    param(
        [hashtable]$Tables,
        [string]$TableName,
        [string]$ColumnName
    )
    if ([string]::IsNullOrWhiteSpace($TableName) -or [string]::IsNullOrWhiteSpace($ColumnName)) {
        return
    }
    Add-Table -Tables $Tables -TableName $TableName
    $cols = $Tables[$TableName].Columns
    if (-not $cols.Contains($ColumnName)) {
        $cols.Add($ColumnName, $true)
    }
}

function Set-TableColumns {
    param(
        [hashtable]$Tables,
        [string]$TableName,
        [string[]]$Columns
    )
    if ([string]::IsNullOrWhiteSpace($TableName)) {
        return
    }
    $orderedCols = New-Object "System.Collections.Specialized.OrderedDictionary"
    foreach ($column in $Columns) {
        if (-not [string]::IsNullOrWhiteSpace($column) -and -not $orderedCols.Contains($column)) {
            $orderedCols.Add($column, $true)
        }
    }
    $Tables[$TableName] = [ordered]@{
        Columns = $orderedCols
    }
}

function Get-PrimaryKeyColumns {
    param(
        [string]$TableName,
        [string[]]$Columns
    )

    $preferred = @(
        "${TableName}_ID",
        "$(Get-SingularName -TableName $TableName)_ID"
    )
    foreach ($candidate in $preferred) {
        if ($Columns -contains $candidate) {
            return @($candidate)
        }
    }

    switch ($TableName) {
        "COMMON_CODE_GROUP" { if ($Columns -contains "CODE_GRP_ID") { return @("CODE_GRP_ID") } }
        "COMMON_CODE" { if ($Columns -contains "CODE_ID") { return @("CODE_ID") } }
        "JOB_GRADE" { if ($Columns -contains "JBGD_CD") { return @("JBGD_CD") } }
        "DEPARTMENT" { if ($Columns -contains "DEPT_ID") { return @("DEPT_ID") } }
        "USERS" { if ($Columns -contains "USER_ID") { return @("USER_ID") } }
    }

    $idColumns = @($Columns | Where-Object { $_ -match "(_ID$|_CD$)" })
    if ($idColumns.Count -eq 1) {
        return $idColumns
    }

    if ($idColumns.Count -ge 2 -and (
            $TableName -match "(MAPPING|MEMBER|RECEIVER|PARTY|READ|CHECKLIST)$"
        )) {
        return $idColumns
    }

    return @()
}

$mapperPath = (Resolve-Path -LiteralPath $MapperDir).Path
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$outputPath = (Resolve-Path -LiteralPath $OutputDir).Path

$tables = @{}
$sequences = New-Object "System.Collections.Generic.HashSet[string]"
$cteNames = New-Object "System.Collections.Generic.HashSet[string]"

$aliasKeywordSet = New-Object "System.Collections.Generic.HashSet[string]"
@(
    "ON", "WHERE", "LEFT", "RIGHT", "INNER", "FULL", "CROSS",
    "GROUP", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION",
    "VALUES", "SET", "WHEN", "THEN", "ELSE", "END"
) | ForEach-Object { [void]$aliasKeywordSet.Add($_) }

$columnKeywordSet = New-Object "System.Collections.Generic.HashSet[string]"
@(
    "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL", "CROSS",
    "ON", "AND", "OR", "NOT", "NULL", "IS", "IN", "EXISTS", "BETWEEN", "LIKE", "AS",
    "GROUP", "ORDER", "BY", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT",
    "CASE", "WHEN", "THEN", "ELSE", "END", "ASC", "DESC", "WITH", "RECURSIVE",
    "INSERT", "INTO", "UPDATE", "DELETE", "VALUES", "SET", "RETURNING",
    "COALESCE", "COUNT", "MAX", "MIN", "SUM", "AVG", "SUBSTR", "TO_NUMBER", "LPAD",
    "CURRENT_TIMESTAMP", "CURRENT_DATE", "NOW", "TRUE", "FALSE"
) | ForEach-Object { [void]$columnKeywordSet.Add($_) }

$mapperFiles = Get-ChildItem -Path $mapperPath -Recurse -Filter *.xml -File
if (-not $mapperFiles) {
    throw "No mapper XML files found under: $mapperPath"
}

foreach ($file in $mapperFiles) {
    $raw = Get-Content -LiteralPath $file.FullName -Raw
    $statementMatches = [regex]::Matches($raw, "(?is)<(select|insert|update|delete)\b[^>]*>(.*?)</\1>")

    foreach ($stmt in $statementMatches) {
        $sqlRaw = $stmt.Groups[2].Value
        $sqlRaw = [regex]::Replace($sqlRaw, "(?is)<!--.*?-->", " ")
        $sqlRaw = [regex]::Replace($sqlRaw, "(?is)/\*.*?\*/", " ")
        $sqlRaw = [regex]::Replace($sqlRaw, "(?m)--.*$", " ")
        $sqlRaw = [regex]::Replace($sqlRaw, "(?is)<[^>]+>", " ")
        $sqlRaw = [regex]::Replace($sqlRaw, "\s+", " ")
        $sql = $sqlRaw.ToUpperInvariant()

        foreach ($seq in [regex]::Matches($sql, "NEXTVAL\s*\(\s*'([A-Z0-9_]+)'\s*\)")) {
            [void]$sequences.Add($seq.Groups[1].Value)
        }
        foreach ($seq in [regex]::Matches($sql, "([A-Z0-9_]+)\.NEXTVAL")) {
            [void]$sequences.Add($seq.Groups[1].Value)
        }

        foreach ($cte in [regex]::Matches($sql, "\bWITH\s+(?:RECURSIVE\s+)?([A-Z_][A-Z0-9_]*)\s+AS\s*\(")) {
            [void]$cteNames.Add($cte.Groups[1].Value)
        }
        foreach ($cte in [regex]::Matches($sql, "\)\s*,\s*([A-Z_][A-Z0-9_]*)\s+AS\s*\(")) {
            [void]$cteNames.Add($cte.Groups[1].Value)
        }

        $aliasMap = @{}

        foreach ($m in [regex]::Matches($sql, "\b(?:FROM|JOIN)\s+(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)\s*(?:AS\s+)?([A-Z_][A-Z0-9_]*)?")) {
            $table = $m.Groups[1].Value
            $alias = $m.Groups[2].Value
            Add-Table -Tables $tables -TableName $table
            $aliasMap[$table] = $table
            if (-not [string]::IsNullOrWhiteSpace($alias) -and -not $aliasKeywordSet.Contains($alias)) {
                $aliasMap[$alias] = $table
            }
        }

        foreach ($m in [regex]::Matches($sql, "\bINSERT\s+INTO\s+(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)")) {
            Add-Table -Tables $tables -TableName $m.Groups[1].Value
        }
        foreach ($m in [regex]::Matches($sql, "\bUPDATE\s+(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)")) {
            Add-Table -Tables $tables -TableName $m.Groups[1].Value
        }
        foreach ($m in [regex]::Matches($sql, "\bDELETE\s+FROM\s+(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)")) {
            Add-Table -Tables $tables -TableName $m.Groups[1].Value
        }

        $statementTables = @($aliasMap.Values | Sort-Object -Unique)
        if ($statementTables.Count -eq 1) {
            $singleTable = $statementTables[0]

            $selectMatch = [regex]::Match($sql, "(?is)\bSELECT\b(.*?)\bFROM\b")
            if ($selectMatch.Success) {
                $selectPart = $selectMatch.Groups[1].Value
                $exprs = $selectPart -split ","
                foreach ($expr in $exprs) {
                    $candidate = $expr.Trim()
                    $candidate = $candidate -replace "^\s*DISTINCT\s+", ""
                    $candidate = $candidate -replace "(?is)\s+AS\s+[A-Z_][A-Z0-9_]*\s*$", ""
                    $candidate = $candidate -replace "(?is)\s+[A-Z_][A-Z0-9_]*\s*$", ""
                    $candidate = $candidate -replace "(?is)\(.*\)", ""
                    $candidate = $candidate.Trim()
                    if ($candidate -match "^[A-Z_][A-Z0-9_]*$" -and -not $columnKeywordSet.Contains($candidate)) {
                        Add-Column -Tables $tables -TableName $singleTable -ColumnName $candidate
                    }
                }
            }

            foreach ($cond in [regex]::Matches($sql, "\b([A-Z_][A-Z0-9_]*)\s*(?:=|<>|>=|<=|>|<|\bIN\b|\bLIKE\b|\bIS\b)")) {
                $candidate = $cond.Groups[1].Value
                if (-not $columnKeywordSet.Contains($candidate)) {
                    Add-Column -Tables $tables -TableName $singleTable -ColumnName $candidate
                }
            }
        }

        foreach ($ins in [regex]::Matches($sql, "(?is)\bINSERT\s+INTO\s+(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)\s*\((.*?)\)\s*(?:VALUES|SELECT)")) {
            $table = $ins.Groups[1].Value
            $columnsPart = $ins.Groups[2].Value
            $parts = $columnsPart -split ","
            foreach ($p in $parts) {
                $token = [regex]::Match($p.Trim(), "^([A-Z_][A-Z0-9_]*)$")
                if ($token.Success) {
                    Add-Column -Tables $tables -TableName $table -ColumnName $token.Groups[1].Value
                }
            }
        }

        foreach ($upd in [regex]::Matches($sql, "(?is)\bUPDATE\s+(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)\s+SET\s+(.*?)(?:\bWHERE\b|\bRETURNING\b|$)")) {
            $table = $upd.Groups[1].Value
            $setPart = $upd.Groups[2].Value
            foreach ($assign in [regex]::Matches($setPart, "(?:^|,)\s*(?:[A-Z_][A-Z0-9_]*\.)?([A-Z_][A-Z0-9_]*)\s*=")) {
                Add-Column -Tables $tables -TableName $table -ColumnName $assign.Groups[1].Value
            }
        }

        foreach ($pair in [regex]::Matches($sql, "\b([A-Z_][A-Z0-9_]*)\.([A-Z_][A-Z0-9_]*)\b")) {
            $alias = $pair.Groups[1].Value
            $column = $pair.Groups[2].Value
            if ($aliasMap.ContainsKey($alias)) {
                Add-Column -Tables $tables -TableName $aliasMap[$alias] -ColumnName $column
            }
        }
    }
}

$ignoreTables = New-Object "System.Collections.Generic.HashSet[string]"
@(
    "APV_DOC", "BASE_DATA", "CMV", "DEPT_TREE", "ID", "MV", "NMV", "SORTED_DATA"
) | ForEach-Object { [void]$ignoreTables.Add($_) }

$tablesToDrop = @()
foreach ($name in $tables.Keys) {
    if ($cteNames.Contains($name) -or $ignoreTables.Contains($name)) {
        $tablesToDrop += $name
    }
}
$tablesToDrop | ForEach-Object { [void]$tables.Remove($_) }

foreach ($tableName in $tables.Keys) {
    $cols = @($tables[$tableName].Columns.Keys)
    if ($cols.Count -eq 0) {
        $fallback = "$(Get-SingularName -TableName $tableName)_ID"
        Add-Column -Tables $tables -TableName $tableName -ColumnName $fallback
    }
}

$coreColumns = @{
    "COMMON_CODE_GROUP" = @("CODE_GRP_ID", "CODE_GRP_NM", "USE_YN")
    "COMMON_CODE"       = @("CODE_ID", "CODE_GRP_ID", "CODE_NM", "USE_YN")
    "JOB_GRADE"         = @("JBGD_CD", "JBGD_NM", "APPR_ATRZ_YN", "CRT_DT", "USE_YN")
    "DEPARTMENT"        = @("DEPT_ID", "DEPT_NM", "USE_YN", "UP_DEPT_ID", "SORT_NUM")
    "USERS"             = @("USER_ID", "USER_PSWD", "USER_NM", "USER_EMAIL", "USER_TELNO", "EXT_TEL", "RSGNTN_YN", "RSGNTN_YMD", "DEPT_ID", "JBGD_CD", "USER_IMG_FILE_ID", "USER_ROLE", "HIRE_YMD", "WORK_STTS_CD")
}
foreach ($tableName in $coreColumns.Keys) {
    Set-TableColumns -Tables $tables -TableName $tableName -Columns $coreColumns[$tableName]
}

$schemaFile = Join-Path $outputPath "schema_postgres.sql"
$constraintsFile = Join-Path $outputPath "constraints_postgres.sql"
$indexesFile = Join-Path $outputPath "indexes_postgres.sql"
$seedFile = Join-Path $outputPath "seed_sample.sql"

$schemaLines = New-Object System.Collections.Generic.List[string]
$schemaLines.Add("-- Auto-generated from MyBatis mapper SQL")
$schemaLines.Add("-- Generated at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$schemaLines.Add("")
$schemaLines.Add("CREATE EXTENSION IF NOT EXISTS pgcrypto;")
$schemaLines.Add("")

foreach ($seq in ($sequences | Sort-Object)) {
    $schemaLines.Add("CREATE SEQUENCE IF NOT EXISTS $seq;")
}
if ($sequences.Count -gt 0) {
    $schemaLines.Add("")
}

foreach ($tableName in ($tables.Keys | Sort-Object)) {
    $columns = @($tables[$tableName].Columns.Keys | Sort-Object)
    $pkColumns = @(Get-PrimaryKeyColumns -TableName $tableName -Columns $columns)

    $schemaLines.Add("CREATE TABLE IF NOT EXISTS $tableName (")
    $columnDefs = New-Object System.Collections.Generic.List[string]
    foreach ($col in $columns) {
        $line = "    $col $(Get-ColumnType -ColumnName $col)"
        if ($pkColumns -contains $col) {
            $line += " NOT NULL"
        }
        if ($col -match "_YN$") {
            $line += " DEFAULT 'N'"
        }
        $columnDefs.Add($line)
    }
    if ($pkColumns.Count -gt 0) {
        $columnDefs.Add("    CONSTRAINT PK_${tableName} PRIMARY KEY (" + ($pkColumns -join ", ") + ")")
    }
    $schemaLines.Add(($columnDefs -join ",`n"))
    $schemaLines.Add(");")
    $schemaLines.Add("")
}

Set-Content -LiteralPath $schemaFile -Value ($schemaLines -join "`r`n") -Encoding UTF8

$constraintLines = @(
    '-- Minimal safe constraints for local bootstrap',
    '-- Apply after schema_postgres.sql',
    '',
    'DO $$ BEGIN',
    '  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''FK_COMMON_CODE_GROUP'') THEN',
    '    ALTER TABLE COMMON_CODE ADD CONSTRAINT FK_COMMON_CODE_GROUP FOREIGN KEY (CODE_GRP_ID) REFERENCES COMMON_CODE_GROUP(CODE_GRP_ID);',
    '  END IF;',
    'END $$;',
    '',
    'DO $$ BEGIN',
    '  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''FK_DEPARTMENT_PARENT'') THEN',
    '    ALTER TABLE DEPARTMENT ADD CONSTRAINT FK_DEPARTMENT_PARENT FOREIGN KEY (UP_DEPT_ID) REFERENCES DEPARTMENT(DEPT_ID);',
    '  END IF;',
    'END $$;',
    '',
    'DO $$ BEGIN',
    '  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''FK_USERS_DEPT'') THEN',
    '    ALTER TABLE USERS ADD CONSTRAINT FK_USERS_DEPT FOREIGN KEY (DEPT_ID) REFERENCES DEPARTMENT(DEPT_ID);',
    '  END IF;',
    'END $$;',
    '',
    'DO $$ BEGIN',
    '  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''FK_USERS_JBGD'') THEN',
    '    ALTER TABLE USERS ADD CONSTRAINT FK_USERS_JBGD FOREIGN KEY (JBGD_CD) REFERENCES JOB_GRADE(JBGD_CD);',
    '  END IF;',
    'END $$;',
    '',
    'DO $$ BEGIN',
    '  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ''FK_USERS_WORK_STTS'') THEN',
    '    ALTER TABLE USERS ADD CONSTRAINT FK_USERS_WORK_STTS FOREIGN KEY (WORK_STTS_CD) REFERENCES COMMON_CODE(CODE_ID);',
    '  END IF;',
    'END $$;',
    ''
)
Set-Content -LiteralPath $constraintsFile -Value ($constraintLines -join "`r`n") -Encoding UTF8

$indexLines = New-Object System.Collections.Generic.List[string]
$indexLines.Add("-- Auto-generated helper indexes")
$indexLines.Add("")
foreach ($tableName in ($tables.Keys | Sort-Object)) {
    $columns = @($tables[$tableName].Columns.Keys | Sort-Object)
    $pkColumns = @(Get-PrimaryKeyColumns -TableName $tableName -Columns $columns)
    foreach ($col in $columns) {
        if ($pkColumns -contains $col) { continue }
        if ($col -match "(_ID$|_CD$|_DT$|_YMD$)") {
            $idx = "IDX_${tableName}_${col}"
            $indexLines.Add("CREATE INDEX IF NOT EXISTS $idx ON $tableName($col);")
        }
    }
}
Set-Content -LiteralPath $indexesFile -Value ($indexLines -join "`r`n") -Encoding UTF8

$seedLines = @(
    "-- Sample seed for local development (no production data migration)",
    "BEGIN;",
    "",
    "INSERT INTO COMMON_CODE_GROUP (CODE_GRP_ID, CODE_GRP_NM, USE_YN)",
    "VALUES",
    "('WORK_STTS', '근무 상태', 'Y'),",
    "('APPR_DOC_STTS', '결재 문서 상태', 'Y'),",
    "('APPR_LINE_STTS', '결재선 상태', 'Y')",
    "ON CONFLICT DO NOTHING;",
    "",
    "INSERT INTO COMMON_CODE (CODE_ID, CODE_GRP_ID, CODE_NM, USE_YN)",
    "VALUES",
    "('C103', 'WORK_STTS', '재직', 'Y'),",
    "('C104', 'WORK_STTS', '휴직', 'Y'),",
    "('D001', 'APPR_DOC_STTS', '대기', 'Y'),",
    "('D002', 'APPR_DOC_STTS', '승인', 'Y'),",
    "('L001', 'APPR_LINE_STTS', '대기', 'Y'),",
    "('L002', 'APPR_LINE_STTS', '승인', 'Y')",
    "ON CONFLICT DO NOTHING;",
    "",
    "INSERT INTO JOB_GRADE (JBGD_CD, JBGD_NM, APPR_ATRZ_YN, CRT_DT, USE_YN)",
    "VALUES",
    "('J001', '사원', 'N', CURRENT_TIMESTAMP, 'Y'),",
    "('J002', '대리', 'N', CURRENT_TIMESTAMP, 'Y'),",
    "('J003', '과장', 'Y', CURRENT_TIMESTAMP, 'Y'),",
    "('J004', '부장', 'Y', CURRENT_TIMESTAMP, 'Y')",
    "ON CONFLICT DO NOTHING;",
    "",
    "INSERT INTO DEPARTMENT (DEPT_ID, DEPT_NM, USE_YN, UP_DEPT_ID, SORT_NUM)",
    "VALUES",
    "('DP001000', '본사', 'Y', NULL, 1),",
    "('DP001001', '운영', 'Y', 'DP001000', 1),",
    "('DP001002', '개발', 'Y', 'DP001000', 2)",
    "ON CONFLICT DO NOTHING;",
    "",
    "INSERT INTO USERS (",
    "  USER_ID, USER_PSWD, USER_NM, USER_EMAIL, USER_TELNO, EXT_TEL,",
    "  RSGNTN_YN, RSGNTN_YMD, DEPT_ID, JBGD_CD, USER_ROLE, HIRE_YMD, WORK_STTS_CD",
    ") VALUES",
    "('admin', '{noop}admin1234', '관리자', 'admin@starworks.local', '010-0000-0000', '1001',",
    " 'N', NULL, 'DP001001', 'J004', 'ROLE_ADMIN', CURRENT_TIMESTAMP, 'C103'),",
    "('user01', '{noop}user1234', '테스트 사용자', 'user01@starworks.local', '010-0000-0001', '1002',",
    " 'N', NULL, 'DP001002', 'J001', 'ROLE_USER', CURRENT_TIMESTAMP, 'C103')",
    "ON CONFLICT DO NOTHING;",
    "",
    "-- 결재 양식 기본 시드 (카테고리 코드는 UI data-category 값과 일치해야 함)",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '휴가 신청서',",
    '  HTML_CONTENTS = $$<section><h2>휴가 신청서</h2><table><tr><th>휴가 종류</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>사유</th><td></td></tr><tr><th>업무 인수자</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'hr',",
    "  ATRZ_DESCRIPTION = '연차/반차/병가 신청 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'VAC_REQ';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'VAC_REQ',",
    "  '휴가 신청서',",
    '  $$<section><h2>휴가 신청서</h2><table><tr><th>휴가 종류</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>사유</th><td></td></tr><tr><th>업무 인수자</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'hr', '연차/반차/병가 신청 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'VAC_REQ');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '출장/외근 신청서',",
    '  HTML_CONTENTS = $$<section><h2>출장/외근 신청서</h2><table><tr><th>방문지</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>목적</th><td></td></tr><tr><th>예상 비용</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'trip',",
    "  ATRZ_DESCRIPTION = '출장 및 외근 승인 요청 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'TRIP_REQ';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'TRIP_REQ',",
    "  '출장/외근 신청서',",
    '  $$<section><h2>출장/외근 신청서</h2><table><tr><th>방문지</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>목적</th><td></td></tr><tr><th>예상 비용</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'trip', '출장 및 외근 승인 요청 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'TRIP_REQ');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '지출 결의서',",
    '  HTML_CONTENTS = $$<section><h2>지출 결의서</h2><table><tr><th>지출 일자</th><td></td></tr><tr><th>금액</th><td></td></tr><tr><th>비용 계정</th><td></td></tr><tr><th>세부 내역</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'finance',",
    "  ATRZ_DESCRIPTION = '비용 집행 및 정산 승인 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'EXP_APPROVAL';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'EXP_APPROVAL',",
    "  '지출 결의서',",
    '  $$<section><h2>지출 결의서</h2><table><tr><th>지출 일자</th><td></td></tr><tr><th>금액</th><td></td></tr><tr><th>비용 계정</th><td></td></tr><tr><th>세부 내역</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'finance', '비용 집행 및 정산 승인 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'EXP_APPROVAL');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '구매 품의서',",
    '  HTML_CONTENTS = $$<section><h2>구매 품의서</h2><table><tr><th>품목</th><td></td></tr><tr><th>수량</th><td></td></tr><tr><th>예산</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'finance',",
    "  ATRZ_DESCRIPTION = '구매 요청 및 예산 집행 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'PO_REQUEST';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'PO_REQUEST',",
    "  '구매 품의서',",
    '  $$<section><h2>구매 품의서</h2><table><tr><th>품목</th><td></td></tr><tr><th>수량</th><td></td></tr><tr><th>예산</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'finance', '구매 요청 및 예산 집행 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'PO_REQUEST');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '프로젝트 기안서',",
    '  HTML_CONTENTS = $$<section><h2>프로젝트 기안서</h2><table><tr><th>프로젝트명</th><td></td></tr><tr><th>목표</th><td></td></tr><tr><th>범위</th><td></td></tr><tr><th>일정</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'pro',",
    "  ATRZ_DESCRIPTION = '프로젝트 실행 기안 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'PRO_PROPOSAL';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'PRO_PROPOSAL',",
    "  '프로젝트 기안서',",
    '  $$<section><h2>프로젝트 기안서</h2><table><tr><th>프로젝트명</th><td></td></tr><tr><th>목표</th><td></td></tr><tr><th>범위</th><td></td></tr><tr><th>일정</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'pro', '프로젝트 실행 기안 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'PRO_PROPOSAL');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '마케팅 실행 요청서',",
    '  HTML_CONTENTS = $$<section><h2>마케팅 실행 요청서</h2><table><tr><th>캠페인명</th><td></td></tr><tr><th>대상</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>기대 KPI</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'sales',",
    "  ATRZ_DESCRIPTION = '영업/마케팅 실행 승인 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'MKT_REQUEST';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'MKT_REQUEST',",
    "  '마케팅 실행 요청서',",
    '  $$<section><h2>마케팅 실행 요청서</h2><table><tr><th>캠페인명</th><td></td></tr><tr><th>대상</th><td></td></tr><tr><th>기간</th><td></td></tr><tr><th>기대 KPI</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'sales', '영업/마케팅 실행 승인 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'MKT_REQUEST');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '개발/IT 작업 요청서',",
    '  HTML_CONTENTS = $$<section><h2>개발/IT 작업 요청서</h2><table><tr><th>시스템</th><td></td></tr><tr><th>요청 내용</th><td></td></tr><tr><th>우선순위</th><td></td></tr><tr><th>희망 완료일</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'it',",
    "  ATRZ_DESCRIPTION = '개발 및 IT 지원 요청 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'IT_WORK_REQ';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'IT_WORK_REQ',",
    "  '개발/IT 작업 요청서',",
    '  $$<section><h2>개발/IT 작업 요청서</h2><table><tr><th>시스템</th><td></td></tr><tr><th>요청 내용</th><td></td></tr><tr><th>우선순위</th><td></td></tr><tr><th>희망 완료일</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'it', '개발 및 IT 지원 요청 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'IT_WORK_REQ');",
    "",
    "UPDATE AUTHORIZATION_DOCUMENT_TEMPLATE",
    "SET",
    "  ATRZ_DOC_TMPL_NM = '물류 이동 요청서',",
    '  HTML_CONTENTS = $$<section><h2>물류 이동 요청서</h2><table><tr><th>출발지</th><td></td></tr><tr><th>도착지</th><td></td></tr><tr><th>품목/수량</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,',
    "  ATRZ_SECURE_LVL = '2',",
    "  ATRZ_SAVE_YEAR = '5',",
    "  ATRZ_CATEGORY = 'logistics',",
    "  ATRZ_DESCRIPTION = '재고 및 물류 이동 요청 문서',",
    "  DEL_YN = 'N'",
    "WHERE ATRZ_DOC_CD = 'LOG_TRANSFER';",
    "",
    "INSERT INTO AUTHORIZATION_DOCUMENT_TEMPLATE (",
    "  ATRZ_DOC_TMPL_ID, ATRZ_DOC_CD, ATRZ_DOC_TMPL_NM, HTML_CONTENTS,",
    "  ATRZ_SECURE_LVL, ATRZ_SAVE_YEAR, ATRZ_CATEGORY, ATRZ_DESCRIPTION, CRT_DT",
    ")",
    "SELECT",
    "  'ATRZDOC' || LPAD(nextval('ATRZ_DOC_TMPL_ID_SEQ')::text, 3, '0'),",
    "  'LOG_TRANSFER',",
    "  '물류 이동 요청서',",
    '  $$<section><h2>물류 이동 요청서</h2><table><tr><th>출발지</th><td></td></tr><tr><th>도착지</th><td></td></tr><tr><th>품목/수량</th><td></td></tr><tr><th>요청 사유</th><td></td></tr></table></section>$$,',
    "  '2', '5', 'logistics', '재고 및 물류 이동 요청 문서', CURRENT_TIMESTAMP",
    "WHERE NOT EXISTS (SELECT 1 FROM AUTHORIZATION_DOCUMENT_TEMPLATE WHERE ATRZ_DOC_CD = 'LOG_TRANSFER');",
    "",
    "COMMIT;"
)
Set-Content -LiteralPath $seedFile -Value ($seedLines -join "`r`n") -Encoding UTF8

Write-Host "Generated files:"
Write-Host " - $schemaFile"
Write-Host " - $constraintsFile"
Write-Host " - $indexesFile"
Write-Host " - $seedFile"
Write-Host ""
Write-Host "Tables discovered: $($tables.Count)"
Write-Host "Sequences discovered: $($sequences.Count)"
