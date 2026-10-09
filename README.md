# BancoIAS Back

###  instrucciones para ejecucción

- Hacer el clonado de los repositorios 
**Backend**
https://github.com/jhons1101/BancoIAS.git

**Frontend**


El proyecto backend requiere java 17 y Mysql
Abrir proyecto con el IDE de preferencia, instalar las dependencias del **pom.xml** y levantar el proyecto.

Una vez leventado, validar el puerto asignado y probar los servicios adjuntos en el **BancoIAS.postman_collection.json**

Para la base de datos, en el proyecto se adjuntaron 2 archivos dentro de la carpeta BancoIAS\src\main\resources
-schema.sql
-data.sql

Estos archivos son los scripts a ejecutar en la DB local mysql para tener la data de entrada.

####En el application.yml. cambiar los datos de conexion a la DB en caso de ser necesario.


##Servicios

> curl --location 'http://localhost:8080/api/accounts' \
--data ''

Este servicio obtiene el saldo de las cuentas y su estado

    [
		{
			"accountId": "CTA-1001",
			"customerId": "USR-10",
			"status": "ACTIVE",
			"balance": 79000.00
		},
		{
			"accountId": "CTA-1002",
			"customerId": "USR-10",
			"status": "BLOCKED",
			"balance": 800000.00
		},
		{
			"accountId": "CTA-2001",
			"customerId": "USR-20",
			"status": "ACTIVE",
			"balance": 2921000.00
		}
    ]
    

> curl --location 'http://localhost:8080/api/transactions'

Este servicio obtiene el listado de las transacciones

    [
		{
			"requestReference": "REF-001",
			"sourceAccountId": "CTA-1001",
			"destinationAccountId": "CTA-2001",
			"amount": 600000.00,
			"status": "PROCESSED",
			"reason": null,
			"processedAt": "2026-10-08T19:34:11"
		},
		{
			"requestReference": "REF-003",
			"sourceAccountId": "CTA-10010",
			"destinationAccountId": "CTA-2001",
			"amount": 120000.00,
			"status": "REJECTED",
			"reason": "La cuenta origen no existe.",
			"processedAt": "2026-10-08T20:08:59"
		}
    ]
    

> curl --location 'http://localhost:8080/api/transactions' \
--header 'Content-Type: application/json' \
--data '{
    "requestReference": "REF-014",
    "sourceAccountId": "CTA-1001",
    "destinationAccountId": "CTA-2001",
    "amount": 1000
}'

Este servicio guarda una transacción

    {
		"requestReference": "REF-014",
		"sourceAccountId": "CTA-1001",
		"destinationAccountId": "CTA-2001",
		"amount": 1000,
		"status": "PROCESSED",
		"reason": null,
		"processedAt": "2026-10-08T22:16:31.6056113"
	}
    

> curl --location 'http://localhost:8080/api/transactions/REF-003' \
--data ''

Este servicio obtiene una transacción por su 'requestReference'

    {
		"requestReference": "REF-003",
		"sourceAccountId": "CTA-10010",
		"destinationAccountId": "CTA-2001",
		"amount": 120000.00,
		"status": "REJECTED",
		"reason": "La cuenta origen no existe.",
		"processedAt": "2026-10-08T20:08:59"
    }
    

##Datos iniciales Mysql (data.sql)
> Tabla: accounts

| account_id  | customer_id  | status | balance |
|:------------:|:---------------:|:-----:|-----:|
| CTA-1001 | USR-10 | ACTIVE | $1000000 |
| CTA-1002 | USR-10 | BLOCKED | $800000 |
| CTA-2001 | USR-20 | ACTIVE | $2000000 |
                


>  Tabla: account_transactions
				
| request_reference | source_account_id  | destination_account_id | amount | status | reason |
|:------------:|:---------------:|:-----:|:-----:|:-----:|:-----:|
| REF-001 | CTA-1001 | CTA-2001 | $600000 | PROCESSED | NULL |
| REF-004 | CTA-1001 | CTA-2001 | $1200000 | REJECTED | La cuenta origen no tiene saldo suficiente para realizar la transferencia. |
----

#### Link para visualizar los registros de IA, desiciones importantes y datos relevantes del desarrollo
https://1drv.ms/o/c/1494a02195ef21e6/IgAljssaGrAcRZPRo4dOplkiAVPOUJd2ybAVwYCPTbywvUA?e=ormy9A

## Repositorio Front
https://github.com/jhons1101/bancoIAS-front

