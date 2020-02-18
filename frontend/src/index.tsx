import * as React from 'react'
import {render} from 'react-dom'
import {BrowserRouter as Router, Route} from "react-router-dom";
import DesktopHome from "./desktop/pages/DesktopHome/DesktopHome";
import './reset.scss'


const router = <Router>
    <Route exact path='/' component={DesktopHome}/>
</Router>;

render(router, document.getElementById('root'));
