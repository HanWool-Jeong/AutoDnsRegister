const async = require("async");
const mariadb = require("mariadb");

let currentIp, connection, cloudflareToken, zoneId, googleAccessToken;
let targetDnsObjs = [];

async.waterfall([
  async function checkInternetAndIp() {
    let response = await fetch("https://ifconfig.me/ip");
    if (!response.ok) throw new Error(`아이피확인api에러: ${JSON.stringify(response)}`);
    currentIp = await response.text();
  },
  async function getCloudflareTokenFromDB() {
    connection = await mariadb.createConnection({
      host: 'localhost',
      user: 'hanwool',
      password: '2341',
      database: "AutoDns"
    });
    let result = await connection.query("SELECT * FROM Token WHERE comment='Cloudflare api token'");
    cloudflareToken = result[0].value;
  },
  async function checkIfCloudflareTokenValid() {
    let response = await fetch("https://api.cloudflare.com/client/v4/user/tokens/verify", {
      headers: {Authorization: `Bearer ${cloudflareToken}`},
    });
    if (!response.ok) throw new Error(`클플토큰유효api에러: ${JSON.stringify(response)}`);
    response = await response.json();
  },
  async function getDnsZoneId() {
    let response = await fetch("https://api.cloudflare.com/client/v4/zones", {
      headers: {Authorization: `Bearer ${cloudflareToken}`},
    });
    if (!response.ok) throw new Error(`존확인api에러: ${JSON.stringify(response)}`);
    response = await response.json();
    zoneId = response.result[0].id;
  },
  async function getDnsTargetRecords() {
    let targetRecords = (await connection.query("SELECT record FROM Target")).map(elem => elem.record);
    let response = await fetch(`https://api.cloudflare.com/client/v4/zones/${zoneId}/dns_records`, {
      headers: {
        Authorization: `Bearer ${cloudflareToken}`,
      },
    });
    if (!response.ok) throw new Error(`DNS레코드확인api에러: ${JSON.stringify(response)}`);
    response = await response.json();
    response.result.forEach(records => {
      if (targetRecords.includes(records.name))
        targetDnsObjs.push(records);
    });
  },
  async function getIpChangedDnsRecords() {
    targetDnsObjs = targetDnsObjs
    .filter(elem => elem.content != currentIp)
    .map(elem => {
      elem.previousIp = elem.content;
      return elem;
    });
  },
  async function updateDns() {
    if (targetDnsObjs.length == 0) return false;
    await async.each(targetDnsObjs,
      async (dns) => {
        let response = await fetch(`https://api.cloudflare.com/client/v4/zones/${zoneId}/dns_records/${dns.id}`, {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${cloudflareToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({content: currentIp})
        });
        if (!response.ok) throw new Error(`DNS업데이트api에러: ${JSON.stringify(response)}`);
        response = await response.json();
      }
    );
    return true;
  },
  async function refreshGoogleApiAccessToken(isIpChanged) {
    if (!isIpChanged) return false;
    let clientId, clientSecret, refreshToken;
    await async.parallel([
      async () => {clientId = (await connection.query(`SELECT value FROM Token WHERE comment='Google app client id';`))[0].value;},
      async () => {clientSecret = (await connection.query(`SELECT value FROM Token WHERE comment='Google app client secret';`))[0].value;},
      async () => {refreshToken = (await connection.query(`SELECT value FROM Token WHERE comment='Google api refresh token';`))[0].value;}
    ]);
    const formData = {
      client_id: clientId,
      client_secret: clientSecret, 
      grant_type: "refresh_token",
      refresh_token: refreshToken
    };
    const formBody = Object.keys(formData)
      .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(formData[key])}`)
      .join('&');
    let response = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: formBody
    });
    if (!response.ok) throw new Error(`구글토큰갱신api에러: ${JSON.stringify(response)}`);
    response = await response.json();
    googleAccessToken = response.access_token;
    await connection.query(`UPDATE Token SET VALUE='${googleAccessToken}' WHERE COMMENT='Google api access token'`);
    return true;
  },
  async function sendEmail(isIpChanged) {
    if (!isIpChanged) return;
    const encodedSubject = `=?UTF-8?B?${Buffer.from("도메인 IP 변경 알림").toString('base64')}?=`;
    let body = "";
    for (let i = 0; i < targetDnsObjs.length; i++) {
      body += targetDnsObjs[i].name + `: ${targetDnsObjs[i].previousIp} => ${currentIp}\n`;
    }
    const encodedBody = Buffer.from(body, 'utf8').toString('base64');
    const message = [
      `From: soink366739@gmail.com`,
      `To: soink366739@gmail.com`,
      `Subject: ${encodedSubject}`,
      `Content-Type: text/plain; charset=utf-8`,
      `Content-Transfer-Encoding: base64`,
      '',
      encodedBody,
    ].join('\n');
    const rawMessage = Buffer.from(message).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    let response = await fetch('https://www.googleapis.com/gmail/v1/users/me/messages/send', {
      method: "POST",
      headers: {
        Authorization: `Bearer ${googleAccessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({raw: rawMessage})
    });
    if (!response.ok) throw new Error(`이메일api에러: ${JSON.stringify(response)}`);
  }
], function callback(error, result) {
  if (connection) connection.end();
  if (error) {
    console.error("에러발생");
    console.error(error);
  } else {
    console.log("실행완료");
    if (targetDnsObjs.length == 0) console.log("아이피 안 바뀜");
    else targetDnsObjs.forEach(elem => {console.log(`아이피 변경됨 ${elem.name}: ${elem.previousIp} => ${currentIp}`)});
  }
});