package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

  Driver driver = ProfileMicroserviceApplication.driver;

  public static void InitProfileDb() {
    String queryStr;

    // try (Session session = ProfileMicroserviceApplication.driver.session()) {
    // try (Transaction trans = session.beginTransaction()) {
    // queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
    // trans.run(queryStr);
    //
    // queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
    // trans.run(queryStr);
    //
    // queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
    // trans.run(queryStr);
    //
    // trans.success();
    // }
    // session.close();
    // }

  }

  @Override
  public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
    DbQueryStatus newProfile = null;
    if (userName == null || fullName == null || password == null || userName.equals("") || fullName.equals("") || password.equals("")) {
      newProfile = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
      return newProfile;
    }

    try (Session session = driver.session()) {
      StatementResult result =
          session.run("MATCH (nProfile: profile{ userName: {user} }) RETURN nProfile",
              Values.parameters("user", userName));
      if (result.hasNext()) {
        newProfile = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return newProfile;
      }
    }

    try (Session session = driver.session()) {

      try (Transaction tx = session.beginTransaction()) {
        tx.run(
            "CREATE (nProfile: profile{ userName: {user}, fullName: {name}, password: {pass} }) - [:created]->(nPlaylist: playlist)",
            Values.parameters("user", userName, "name", fullName, "pass", password));
        tx.success();
        newProfile = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
      }
    }
    return newProfile;
  }

  @Override
  public DbQueryStatus followFriend(String userName, String friendUserName) {
    DbQueryStatus status = null;
    if (userName == null || friendUserName == null || (userName.equals(friendUserName)) || userName.equals("") || friendUserName.equals("")) {
      status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
      return status;
    }
    
    try (Session session = driver.session()) {
      StatementResult result =
          session.run("MATCH (nProfile: profile{ userName: {user} }) RETURN nProfile",
              Values.parameters("user", userName));
      if (!result.hasNext()) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }
    try (Session session = driver.session()) {
      StatementResult result =
          session.run("MATCH (nProfile: profile{ userName: {user} }) RETURN nProfile",
              Values.parameters("user", friendUserName));
      if (!result.hasNext()) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }
    
    try (Session session = driver.session()) {
      StatementResult result = session.run(
          "MATCH (user: profile{userName:{follower}}) -[r: follows]-> (friend: profile{userName:{followed}})"
              + "RETURN r",
          Values.parameters("follower", userName, "followed", friendUserName));

      if (result.hasNext()) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }

    try (Session session = driver.session()) {
      StatementResult result = session.run(
          "MATCH (user: profile{userName:{follower}}), (friend: profile{userName:{followed}})"
              + "CREATE (user)-[r: follows]->(friend)" + "RETURN r",
          Values.parameters("follower", userName, "followed", friendUserName));

      if (result.hasNext()) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
        return status;
      } else {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }
  }

  @Override
  public DbQueryStatus unfollowFriend(String userName, String friendUserName) {
    DbQueryStatus status = null;

    if (userName == null || friendUserName == null || (userName.equals(friendUserName))) {
      status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
      return status;
    }
    
    try (Session session = driver.session()) {
      StatementResult result =
          session.run("MATCH (nProfile: profile{ userName: {user} }) RETURN nProfile",
              Values.parameters("user", userName));
      if (!result.hasNext()) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }
    
    try (Session session = driver.session()) {
      StatementResult result =
          session.run("MATCH (nProfile: profile{ userName: {user} }) RETURN nProfile",
              Values.parameters("user", friendUserName));
      if (!result.hasNext()) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }
    
    
    try (Session session = driver.session()) {
      StatementResult result = session.run(
          "MATCH (user: profile{userName:{follower}}) -[r: follows]-> (friend: profile{userName:{followed}})"
              + "RETURN r",
          Values.parameters("follower", userName, "followed", friendUserName));

      if (!(result.hasNext())) {
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return status;
      }
    }

    try (Session session = driver.session()) {
      try (Transaction tx = session.beginTransaction()) {
        tx.run(
            "MATCH (user: profile{userName:{follower}}) -[r: follows]-> (friend: profile{userName:{followed}})"
                + "DELETE r",
            Values.parameters("follower", userName, "followed", friendUserName));
        tx.success();
        status = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
      }
    }
    return status;
  }

  @Override
  public DbQueryStatus getAllSongFriendsLike(String userName) {
    DbQueryStatus songList = null;
    Map<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();

    if (userName == null) {
      songList = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
      songList.setData(data);
      return songList;
    }
    
    try (Session session = driver.session()) {
      StatementResult result =
          session.run("MATCH (nProfile: profile{ userName: {user} }) RETURN nProfile",
              Values.parameters("user", userName));
      if (!result.hasNext()) {
        songList = new DbQueryStatus("", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
        return songList;
      }
    }
    
    try (Session session = driver.session()) {
      StatementResult result = session.run(
          "MATCH (me: profile{userName:{user}}) -[:follows]-> (friends: profile) RETURN friends",
          Values.parameters("user", userName));
      while (result.hasNext()) {
        Record friends = result.next();
        ArrayList<String> songNames = new ArrayList<String>();
        String friendUser = (String) friends.fields().get(0).value().asMap().get("userName");
        
        StatementResult result2 = session.run(
            "MATCH (me: profile{userName:{friend}}) -[:created]-> (: playlist) -[:includes]->( songs : song) RETURN songs",
            Values.parameters("friend", friendUser));
        
        while(result2.hasNext()) {
          Record songs = result2.next();
          String temp = (String) songs.fields().get(0).value().asMap().get("songId");
          songNames.add((String) songs.fields().get(0).value().asMap().get("songId"));
        }
        data.put((String) friends.fields().get(0).value().asMap().get("fullName"), songNames);
      }
      songList = new DbQueryStatus("", DbQueryExecResult.QUERY_OK);
      songList.setData(data);
      return songList;
    }
  }
}
