

![][image1]

DISCIPLINA: Paradigmas de Linguagens de Programação \- CC25B	

PROFESSOR: Djonatham Cauã Fritzen Christ

# **Trabalho Final**

Frameworks/Softwares utilizados:

* Java (JDK 26\)  
* IntelliJ IDEA  
* Maven  
* Spring Boot (Spring Data JpA)  
* PostgreSQL/pgAdmin  
* Segurança: JWT e Basic Auth  
* Insomnia/Postman

Spring Initializr Dependencies:

* Spring Data JPA  
* PostgreSQL Driver  
* Spring Web  
* Lombok

Explicação das pastas principais do projeto:

* **config**: Centraliza as classes responsáveis por customizar o comportamento global do ecossistema Spring (como segurança, CORS, internacionalização e gerenciamento de Beans de terceiros). Estabelece as regras de funcionamento transversais que afetam o comportamento de todas as outras camadas.

* **controller**: Ponto de entrada para as requisições HTTP, expondo os endpoints da API através de anotações como @RestController. É responsável por interceptar chamadas, extrair parâmetros, desserializar payloads (conversão de JSON para objetos Java) e definir o código de status HTTP de retorno (ResponseEntity). Ela não processa regras de negócio, funcionando estritamente como um despachante de requisições para a camada de serviço.

* **service**: Encapsula as regras de negócio, validações lógicas e políticas operacionais do sistema. É onde ocorre o fluxo de dados e o controle transacional da aplicação (gerenciado pela anotação @Transactional). Ela atua de forma totalmente agnóstica ao protocolo de transporte (HTTP) ou ao banco de dados específico, garantindo que as regras do ecossistema possam ser testadas e reutilizadas isoladamente.

* **repository:** Abstrai a lógica de persistência e comunicação com o banco de dados relacional. Ao estender interfaces do Spring Data (como JpaRepository), elimina a necessidade de codificar manualmente conexões JDBC e instruções SQL, utilizando mapeamento nativo ou Query Methods. Funciona como um mecanismo de isolamento arquitetural, impedindo que detalhes de infraestrutura de banco de dados vazem para a lógica de negócio.

* **model**: Contém as entidades de domínio da aplicação que representam as tabelas físicas no banco de dados, mapeadas via ORM (JPA/Hibernate) por meio de anotações como @Entity e @Table. Ela define o esquema estrutural do banco (colunas, tipos de dados e restrições de integridade) e estabelece os relacionamentos entre tabelas (@ManyToMany, @ManyToOne). Serve como a estrutura base de dados que trafega entre as camadas do sistema.

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAnAAAAAFCAYAAADFY9H4AAABgklEQVR4Xu3aMUoDURAG4B8ECwvP4BkEC8FCwULwEF5BsBAsFCwECysPoLWFkEKwEBQUDO7bpLEVLyBYCBYi/r5FFnHGKuj/QjIDX7ZIsY+F92b+ZMGE94wDeM4ei6pxla+XxdTo5OtRYbt5HTvFJGxka0VVWM4Wi7nDHPuYKeoek+wB+XmEgLw3y7FrKaEqrFlDsx/7hbTPoIuoUS4OPsCFEELrlTZg6d3Shiy9c/qQpVVjnzZoaW3Rhiy9VdqgpZSwQBuy1LqYZjtMhpHUfGz/sgG/JBzQHg5qFS7oD0mdChV9o1B7o2+YIYQQwrD6oO9lan3anq53TDvX/IUah9/T3LD/DG6/Uyh9/+Ex5dKdWg/zLuUq1VihT/lqmy5kKSXs0R4iemf0B6TaDX2jUHuhb5ghhPHxZP9RjYr6UWwGuGagL/k+R3PvNlTYd01U/FBbjg1bCnYNYXxdZyeYoA1ZagmztEFLKWGJPmRp1VinDVpqPmSpndKHLLUH+pD133qfkbU1oTfKeokAAAAASUVORK5CYII=>