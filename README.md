# AutoDnsRegister

서버 부팅 시, 클라우드 플레어 도메인에 등록된 IP를 서버의 IP로 바꿔주는 스크립트입니다.<br>
도메인에 등록된 IP가 고정 IP면 좋겠지만 가끔 통신사에서 IP를 바꿔서 만들었습니다.

## 부팅 systemd unit
리눅스 부팅 시 스크립트를 자동으로 실행하는 서비스 유닛.

[systemd unit](docs/auto-dns-register.service)

## DB
![Diagram](https://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/HanWool-Jeong/AutoDnsRegister/main/docs/database.pu&index=0)

[SQL](docs/database.sql)


## 흐름도
### AutoDnsRegisterApplication.java (스프링 서버)
![Diagram](https://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/HanWool-Jeong/AutoDnsRegister/main/docs/AutoDnsRegisterApplication.pu)

### AutoDnsRegisterScript.java (스크립트)
![Diagram](https://www.plantuml.com/plantuml/proxy?src=https://raw.githubusercontent.com/HanWool-Jeong/AutoDnsRegister/main/docs/AutoDnsRegisterScript.pu)