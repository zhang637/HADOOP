package pl.com.sages.hbase.api.loader;

import pl.com.sages.hbase.api.dao.UsersDao;
import pl.com.sages.hadoop.data.model.users.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UserDataFactory {

    public static void insertTestData() throws IOException {
        UsersDao usersDao = new UsersDao();

        InputStream inputStream = UserDataFactory.class.getClassLoader().getResourceAsStream("users/users.csv");

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String delimeter = ";";

        while ((line = br.readLine()) != null) {

            String[] userData = line.split(delimeter);

            User user = new User();
            user.setForename(userData[0]);
            user.setSurname(userData[1]);
            user.setEmail(userData[2]);
            user.setPassword(userData[3]);

            usersDao.save(user);

        }

        br.close();
    }

}
