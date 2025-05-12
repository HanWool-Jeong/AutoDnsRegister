const express = require("express");
const mariadb = require("mariadb");

let clientId, clientSecret;
const redirectUri = "http://localhost:3000/redirect";

const app = express();

app.get("/redirect", async (req, res) => {
  console.log("왔당~~~~");
  const tokenEndpoint = "https://oauth2.googleapis.com/token";
  let formData = {
    client_id: clientId,
    client_secret: clientSecret,
    code: req.query.code,
    grant_type: "authorization_code",
    redirect_uri: redirectUri,
  };
  const formBody = Object.keys(formData)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(formData[key])}`)
    .join('&');
  let response = await fetch(tokenEndpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: formBody
  });
  response = await response.json();
  console.log("결과!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
  console.log(response);
  res.send("리다이렉트됨~~~!");

  let connection = await mariadb.createConnection({
      host: 'localhost',
      user: 'hanwool',
      password: '2341',
      database: "AutoDns"
  });
  await connection.query(`UPDATE Token SET VALUE='${response.access_token}' WHERE COMMENT='Google api access token'`);
  await connection.query(`UPDATE Token SET VALUE='${response.refresh_token}' WHERE COMMENT='Google api refresh token';`);
  connection.end();
});

app.listen(3000, async () => {
  console.log("서버시작");
  let connection = await mariadb.createConnection({
      host: 'localhost',
      user: 'hanwool',
      password: '2341',
      database: "AutoDns"
  });
  clientId = (await connection.query(`SELECT value FROM Token WHERE comment='Google app client id';`))[0].value;
  clientSecret = (await connection.query(`SELECT value FROM Token WHERE comment='Google app client secret';`))[0].value;
  connection.end();
  const oAuthEndpoint = "https://accounts.google.com/o/oauth2/v2/auth";
  const formData = {
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: "code",
    scope: "https://www.googleapis.com/auth/gmail.send",
    access_type: "offline"
  };
  const formBody = Object.keys(formData)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(formData[key])}`)
    .join('&');
  const requestUrl = oAuthEndpoint + "?" + formBody;
  console.log(requestUrl);
});