package com.github.framiere;

import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CdcChangeTest {
    public CdcChange cdcChange = new CdcChange();

    @Test
    public void update() throws IOException {
        assertThat(cdcChange.toTelegraf("" +
                "{\n" +
                "  \"before\": {\n" +
                "     \"id\": 60,\n" +
                "     \"name\": \"California horses\"\n" +
                "  },\n" +
                "  \"after\": {\n" +
                "     \"id\": 60,\n" +
                "     \"name\": \"New Mexico penguins\"\n" +
                "  },\n" +
                "  \"source\": {\n" +
                "     \"version\": \"0.7.3\",\n" +
                "     \"name\": \"dbserver1\",\n" +
                "     \"server_id\": 223344,\n" +
                "     \"ts_sec\": 1520202662,\n" +
                "     \"gtid\": null,\n" +
                "     \"file\": \"mysql-bin.000003\",\n" +
                "     \"pos\": 329994,\n" +
                "     \"row\": 0,\n" +
                "     \"snapshot\": null,\n" +
                "     \"thread\": 2,\n" +
                "     \"db\": \"mydb\",\n" +
                "     \"table\": \"Team\"\n" +
                "  },\n" +
                "  \"op\": \"u\",\n" +
                "  \"ts_ms\": 1520202662784\n" +
                "}")).isEqualTo("cdc,table=Team,operation=update,id=60,name=updated nbChanges=1,found=1 1520202662784000000");
    }

    @Test
    public void delete() {
        assertThat(cdcChange.toTelegraf("" +
                "{\n" +
                "   \"before\": {\n" +
                "      \"id\": 46,\n" +
                "      \"name\": \"Louisiana warlocks\"\n" +
                "   },\n" +
                "   \"after\": null,\n" +
                "   \"source\": {\n" +
                "      \"version\": \"0.7.3\",\n" +
                "      \"name\": \"dbserver1\",\n" +
                "      \"server_id\": 223344,\n" +
                "      \"ts_sec\": 1520202505,\n" +
                "      \"gtid\": null,\n" +
                "      \"file\": \"mysql-bin.000003\",\n" +
                "      \"pos\": 293861,\n" +
                "      \"row\": 0,\n" +
                "      \"snapshot\": null,\n" +
                "      \"thread\": 2,\n" +
                "      \"db\": \"mydb\",\n" +
                "      \"table\": \"Team\"\n" +
                "   },\n" +
                "   \"op\": \"d\",\n" +
                "   \"ts_ms\": 1520202505258\n" +
                "}\n")).isEqualTo("cdc,table=Team,operation=delete,id=46 found=1 1520202505258000000");
    }

    @Test
    public void insert() {
        assertThat(cdcChange.toTelegraf("" +
                "{\n" +
                "   \"before\": null,\n" +
                "   \"after\": {\n" +
                "      \"id\": 57,\n" +
                "      \"name\": \"Florida dragons\"\n" +
                "   },\n" +
                "   \"source\": {\n" +
                "      \"version\": \"0.7.3\",\n" +
                "      \"name\": \"dbserver1\",\n" +
                "      \"server_id\": 223344,\n" +
                "      \"ts_sec\": 1520202543,\n" +
                "      \"gtid\": null,\n" +
                "      \"file\": \"mysql-bin.000003\",\n" +
                "      \"pos\": 301020,\n" +
                "      \"row\": 0,\n" +
                "      \"snapshot\": null,\n" +
                "      \"thread\": 2,\n" +
                "      \"db\": \"mydb\",\n" +
                "      \"table\": \"Team\"\n" +
                "   },\n" +
                "   \"op\": \"c\",\n" +
                "   \"ts_ms\": 1520202543833\n" +
                "}")).isEqualTo("cdc,table=Team,operation=insert,id=57 found=1 1520202543833000000");
    }
}
