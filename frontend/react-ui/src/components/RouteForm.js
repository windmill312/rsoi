import React from 'react';
import {Table, Alert, TabContent, TabPane, Nav, NavItem, NavLink, Button, Row, Col, Label, Form, Input, FormGroup} from 'reactstrap'
import classnames from 'classnames';
import axios from "axios";

class RouteForm extends React.Component {

    constructor() {
        super()
        this.state = {
            activeSubTab: '0',
            routes: [],
            serviceAvailable: true
        }
    }

    componentDidMount() {
        axios.get(`http://localhost:8090/pingRoutes`)
            .then((result) => {
                if (result.status === 200) {
                    axios.get('http://localhost:8090/routes?size=50')
                        .then(
                            response => this.setState({
                                    routes: response.data,
                                    serviceAvailable: true
                                }
                            ))
                        .catch((error) => {
                            console.error(error);
                        });
                } else {
                    console.log('Ошибка при получении маршрутов (status = ' + result.status + ')');
                }
            })
            .catch(error => {
                console.error(error);
                this.setState({
                    serviceAvailable: false
                })
            })
    }



    toggle1(tab) {
        this.componentDidMount();
        if (this.state.activeSubTab !== tab) {
            this.setState({
                activeSubTab: tab
            });
        }
    }

    handleRouteNmChange = event => {
        this.setState({ routeName: event.target.value })
    }

    handleSubmitRoute = event => {
        event.preventDefault();
        const requestData = {
            routeName: this.state.routeName
        };
        axios.put(`http://localhost:8090/route`, requestData)
            .then((result) => {
                if (result.status === 200) {
                    console.info('status = 200');
                    alert('Маршрут успешно создан!');
                } else {
                    console.info('status = ' + result.status);
                    alert('Ошибка при создании маршрута!');
                }
            });
    }

    handleDeleteRoute(route) {
        return event => {
            event.preventDefault();
            axios.delete(`http://localhost:8090/route`, {data: route.uid})
                .then(result => {
                    if (result.status === 200) {
                        console.info('status = 200');
                        alert('Маршрут успешно удален!');
                    } else {
                        console.info('status = ' + result.status);
                        alert('Произошла ошибка при удалении маршрута!');
                    }
                })
        }
    }

    render() {

        if (this.state.serviceAvailable) {
            return (
                <div>
                    <Nav>
                        <NavItem>
                            <NavLink
                                className={classnames({active: this.state.activeSubTab === '0'})}
                                onClick={() => {
                                    this.toggle1('0');
                                }}
                            >
                                Маршруты
                            </NavLink>
                        </NavItem>
                        <NavItem>
                            <NavLink
                                className={classnames({active: this.state.activeSubTab === '1'})}
                                onClick={() => {
                                    this.toggle1('1');
                                }}
                            >
                                Добавить маршрут
                            </NavLink>
                        </NavItem>
                    </Nav>
                    <TabContent activeTab={this.state.activeSubTab}>
                        <TabPane tabId="0">
                            <Row>
                                <Col sm="12">

                                    <Table id="tableId" className="table" size="sm">
                                        <thead>
                                        <tr>
                                            <th> Уникальный номер маршрута</th>
                                            <th> Направление</th>
                                            <th></th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {this.state.routes.map((route) =>
                                            <tr>
                                                <td> {route.uid}</td>
                                                <td> {route.routeName} </td>
                                                <td><Button color="danger"
                                                            onClick={this.handleDeleteRoute(route)}>Удалить</Button>
                                                </td>
                                            </tr>
                                        )}
                                        </tbody>
                                    </Table>
                                </Col>
                            </Row>
                        </TabPane>
                        <TabPane tabId="1">
                            <Row>
                                <Col sm="6">
                                    <Form onSubmit={this.handleSubmitRoute}>
                                        <FormGroup row>
                                            <Label id="lblNmRoute" for="inputNmRoute" sm="5">Направление</Label>
                                            <Col>
                                                <Input name="inputNmRoute" id="inputNmRoute"
                                                       placeholder="Введите направление маршрута"
                                                       onChange={this.handleRouteNmChange}/>
                                            </Col>
                                        </FormGroup>
                                        <FormGroup row>
                                            <Button id="btnCreateRoute" type="submit">Добавить</Button>
                                        </FormGroup>
                                    </Form>
                                </Col>
                            </Row>
                        </TabPane>
                    </TabContent>
                </div>
            )
        } else {
            return (
                <div>
                    <Alert color="danger">Сервис временно недоступен</Alert>
                    <Button outline onClick={()=> {this.componentDidMount(); this.render();}}>Обновить</Button>
                </div>
            )
        }
    }
}

export default RouteForm;