package org.ecocean.rest.admin;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ecocean.Individual;
import org.ecocean.encounter.Encounter;
import org.ecocean.encounter.EncounterFactory;
import org.ecocean.media.MediaAssetFactory;
import org.ecocean.rest.SimpleUser;
import org.ecocean.rest.search.EncounterSearch;
import org.ecocean.rest.search.IndividualSearch;
import org.ecocean.rest.search.SearchController;
import org.ecocean.rest.search.SearchData;
import org.ecocean.rest.search.UserSearch;
import org.ecocean.security.UserFactory;
import org.ecocean.servlet.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.samsix.database.Database;
import com.samsix.database.DatabaseException;
import com.samsix.database.GroupedSqlCondition;
import com.samsix.database.SqlRelationType;
import com.samsix.database.SqlStatement;
import com.samsix.database.SqlTable;

@RestController
@RequestMapping(value = "/admin/search")
public class AdminSearchController {
    Logger logger = LoggerFactory.getLogger(SearchController.class);

    private static void addEncounterData(final SqlStatement sql, final EncounterSearch search) {
        if (search.encdate != null) {
            sql.addCondition(EncounterFactory.ALIAS_ENCOUNTERS, "encdate", SqlRelationType.EQUAL, search.encdate.toString());
        }

        if (!StringUtils.isBlank(search.location)) {
            SqlTable table = sql.findTable(EncounterFactory.ALIAS_ENCOUNTERS);
            GroupedSqlCondition cond = GroupedSqlCondition.orGroup();
            cond.addContainsCondition(table, "locationid", search.location);
            cond.addContainsCondition(table, "verbatimlocation", search.location);
            sql.addCondition(cond);
        }

        if (!StringUtils.isBlank(search.comments)) {
            sql.addContainsCondition(EncounterFactory.ALIAS_ENCOUNTERS, "comments", search.comments);
        }
    }


    public static List<Encounter> searchEncounters(final Database db,
                                                   final SearchData search) throws DatabaseException {
        SqlStatement sql = EncounterFactory.getEncounterStatement();

        addEncounterData(sql, search.encounter);
        //
        // Individual table is already in the basic encounter statement.
        //
        addIndividualData(sql, search.individual);

        if (search.contributor.hasData()) {
            addContributorData(sql, search.contributor);
        }

        return db.selectList(sql, (rs) -> {
            return EncounterFactory.readEncounter(rs);
        });
    }

    private static void addIndividualData(final SqlStatement sql, final IndividualSearch search) {
        if (!StringUtils.isBlank(search.nameid)) {
            SqlTable table = sql.findTable(EncounterFactory.ALIAS_INDIVIDUALS);
            GroupedSqlCondition cond = GroupedSqlCondition.orGroup();
            cond.addContainsCondition(table, "alternateid", search.nameid);
            cond.addContainsCondition(table, "nickname", search.nameid);
            sql.addCondition(cond);
        }

        if (!StringUtils.isBlank(search.species)) {
            sql.addCondition(EncounterFactory.ALIAS_INDIVIDUALS, "species", SqlRelationType.EQUAL, search.species);
        }

        if (!StringUtils.isBlank(search.comments)) {
            sql.addContainsCondition(EncounterFactory.ALIAS_INDIVIDUALS, "comments", search.comments);
        }

        if (!StringUtils.isBlank(search.sex)) {
            sql.addContainsCondition(EncounterFactory.ALIAS_INDIVIDUALS, "sex", search.sex);
        }

        if (search.identified != null) {
            sql.addCondition(EncounterFactory.ALIAS_INDIVIDUALS, "identified", search.identified);
        }
    }

    private static void addUserData(final SqlStatement sql, final SqlTable users, final UserSearch search) {
        if (! StringUtils.isBlank(search.name)) {
            GroupedSqlCondition cond = GroupedSqlCondition.orGroup();
            cond.addContainsCondition(users, "fullname", search.name);
            cond.addContainsCondition(users, "username", search.name);
            sql.addCondition(cond);
        }
    }


    private static void addContributorData(final SqlStatement sql, final UserSearch search) {
        sql.addInnerJoin(EncounterFactory.ALIAS_ENCOUNTERS,
                EncounterFactory.PK_ENCOUNTERS,
                EncounterFactory.TABLENAME_ENCOUNTER_MEDIA,
                EncounterFactory.ALIAS_ENCOUNTER_MEDIA,
                EncounterFactory.PK_ENCOUNTERS);
        sql.addInnerJoin(EncounterFactory.ALIAS_ENCOUNTER_MEDIA,
                         "mediaid",
                         MediaAssetFactory.TABLENAME_MEDIAASSET,
                         "masearch",
                         MediaAssetFactory.PK_MEDIAASSET);
        SqlTable contributors = new SqlTable(UserFactory.TABLENAME_USERS, "contrib");
        SqlTable masearch = sql.findTable("masearch");
        sql.addInnerJoin(masearch, contributors, "submitterid", UserFactory.PK_USERS);

        addUserData(sql, contributors, search);
    }


    public static List<Individual> searchIndividuals(final HttpServletRequest request,
                                                     final SearchData search)
            throws DatabaseException {
        SqlStatement sql = EncounterFactory.getIndividualStatement();

        addIndividualData(sql, search.individual);

        if (search.encounter.hasData() || search.contributor.hasData()) {
            sql.addLeftOuterJoin(EncounterFactory.ALIAS_INDIVIDUALS,
                                 EncounterFactory.PK_INDIVIDUALS,
                                 EncounterFactory.TABLENAME_ENCOUNTERS,
                                 EncounterFactory.ALIAS_ENCOUNTERS,
                                 EncounterFactory.PK_ENCOUNTERS);
            addEncounterData(sql, search.encounter);

            if (search.contributor.hasData()) {
                addContributorData(sql, search.contributor);
            }
        }

        try (Database db = ServletUtils.getDb(request)) {
            return db.selectList(sql, (rs) -> {
                return EncounterFactory.readIndividual(rs);
            });
        }
    }


    @RequestMapping(value = "/encounter", method = RequestMethod.POST)
    public List<Encounter> searchEncounter(final HttpServletRequest request,
                                           @RequestBody final SearchData search) throws DatabaseException
    {
        try (Database db = ServletUtils.getDb(request)) {
            return searchEncounters(db, search);
        }
    }


    @RequestMapping(value = "/individual", method = RequestMethod.POST)
    public List<Individual> searchIndividual(final HttpServletRequest request,
                                             @RequestBody
                                             final SearchData search) throws DatabaseException
    {
        return searchIndividuals(request, search);
    }


    //
    // Returning SimpleUser for now as I don't see the need for a full user.
    // If we need a full user we can get them individually with a call.
    //
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public List<SimpleUser> searchUser(final HttpServletRequest request,
                                       @RequestBody
                                       final UserSearch search) throws DatabaseException
    {
        try (Database db = ServletUtils.getDb(request)) {
            SqlStatement sql = UserFactory.getUserStatement();

            addUserData(sql, sql.findTable(UserFactory.AlIAS_USERS), search);

            sql.setOrderBy("fullname");

            return db.selectList(sql, (rs) -> {
                return UserFactory.readSimpleUser(rs);
            });
        }
    }
}
